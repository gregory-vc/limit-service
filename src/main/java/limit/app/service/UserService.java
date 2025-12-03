package limit.app.service;

import limit.app.config.LimitServiceProperties;
import limit.app.exception.InsufficientLimitException;
import limit.app.exception.InvalidRequestException;
import limit.app.exception.ReservationInvalidStateException;
import limit.app.exception.ReservationNotFoundException;
import limit.app.exception.UserLimitNotFoundException;
import limit.app.repository.LimitOperationRepository;
import limit.app.repository.LimitReservationRepository;
import limit.app.repository.UserLimitRepository;
import limit.app.repository.UserRepository;
import limit.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserLimitRepository userLimitRepository;
    private final LimitReservationRepository reservationRepository;
    private final LimitOperationRepository operationRepository;
    private final LimitServiceProperties properties;
    private final TransactionTemplate transactionTemplate;

    public UserService(UserRepository userRepository,
                       UserLimitRepository userLimitRepository,
                       LimitReservationRepository reservationRepository,
                       LimitOperationRepository operationRepository,
                       LimitServiceProperties properties,
                       TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.userLimitRepository = userLimitRepository;
        this.reservationRepository = reservationRepository;
        this.operationRepository = operationRepository;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    public LimitReservation reserveLimit(String externalUserId, BigDecimal amount, String requestId) {
        validateAmount(amount);
        if (requestId == null || requestId.isBlank()) {
            throw new InvalidRequestException("requestId must be provided");
        }

        var existing = reservationRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return transactionTemplate.execute(status -> createReservation(externalUserId, amount, requestId));
        } catch (DataIntegrityViolationException e) {
            return reservationRepository.findByRequestId(requestId)
                    .orElseThrow(() -> new InvalidRequestException("Failed to create reservation for requestId " + requestId));
        }
    }

    @Transactional
    public LimitReservation confirmLimitAndDebit(Long reservationId) {
        var reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation %d not found".formatted(reservationId)));

        if (reservation.getStatus() != LimitReservationStatus.RESERVED) {
            throw new ReservationInvalidStateException("Reservation %d is not in RESERVED status".formatted(reservationId));
        }

        var userLimit = userLimitRepository.findByUserIdForUpdate(reservation.getUser().getId())
                .orElseThrow(() -> new UserLimitNotFoundException("User limit not found for user " + reservation.getUser().getId()));

        if (userLimit.getReservedAmount().compareTo(reservation.getAmount()) < 0) {
            throw new ReservationInvalidStateException("Reserved amount is insufficient to confirm reservation " + reservationId);
        }

        userLimit.setReservedAmount(userLimit.getReservedAmount().subtract(reservation.getAmount()));
        userLimitRepository.save(userLimit);

        reservation.setStatus(LimitReservationStatus.CONFIRMED);
        reservationRepository.saveAndFlush(reservation);

        operationRepository.save(
                new LimitOperation(
                        reservation.getUser(),
                        reservation,
                        LimitOperationType.DEBIT,
                        reservation.getAmount().negate(),
                        null
                )
        );

        return reservation;
    }

    @Transactional
    public LimitReservation cancelReservation(Long reservationId) {
        var reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation %d not found".formatted(reservationId)));

        if (reservation.getStatus() != LimitReservationStatus.RESERVED) {
            throw new ReservationInvalidStateException("Reservation %d cannot be cancelled from status %s".formatted(
                    reservationId, reservation.getStatus()));
        }

        var userLimit = userLimitRepository.findByUserIdForUpdate(reservation.getUser().getId())
                .orElseThrow(() -> new UserLimitNotFoundException("User limit not found for user " + reservation.getUser().getId()));

        userLimit.setReservedAmount(userLimit.getReservedAmount().subtract(reservation.getAmount()));
        userLimit.setAvailableLimit(userLimit.getAvailableLimit().add(reservation.getAmount()));
        userLimitRepository.save(userLimit);

        reservation.setStatus(LimitReservationStatus.CANCELLED);
        reservationRepository.saveAndFlush(reservation);

        operationRepository.save(
                new LimitOperation(
                        reservation.getUser(),
                        reservation,
                        LimitOperationType.RELEASE,
                        reservation.getAmount(),
                        null
                )
        );

        return reservation;
    }

    private User ensureUser(String externalUserId) {
        return userRepository.findWithLockingByExternalId(externalUserId)
                .orElseGet(() -> {
                    try {
                        return userRepository.saveAndFlush(new User(externalUserId, null, null));
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findWithLockingByExternalId(externalUserId)
                                .orElseThrow(() -> new InvalidRequestException("Failed to create user " + externalUserId));
                    }
                });
    }

    private UserLimit ensureUserLimitWithLock(User user) {
        return userLimitRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> {
                    try {
                        return userLimitRepository.saveAndFlush(
                                new UserLimit(
                                        user,
                                        properties.getDefaultValue(),
                                        BigDecimal.ZERO,
                                        null,
                                        null
                                )
                        );
                    } catch (DataIntegrityViolationException e) {
                        return userLimitRepository.findByUserIdForUpdate(user.getId())
                                .orElseThrow(() -> new InvalidRequestException("Failed to create user limit for user " + user.getId()));
                    }
                });
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Amount must be positive");
        }
    }

    private LimitReservation createReservation(String externalUserId, BigDecimal amount, String requestId) {
        var user = ensureUser(externalUserId);
        var userLimit = ensureUserLimitWithLock(user);

        BigDecimal newAvailable = userLimit.getAvailableLimit().subtract(amount);
        if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientLimitException("Insufficient available limit for reservation");
        }

        userLimit.setAvailableLimit(newAvailable);
        userLimit.setReservedAmount(userLimit.getReservedAmount().add(amount));
        userLimitRepository.saveAndFlush(userLimit);

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(properties.getReservationTtl());

        var reservation = reservationRepository.saveAndFlush(
                new LimitReservation(user, amount, LimitReservationStatus.RESERVED, requestId, expiresAt, null, null)
        );

        operationRepository.save(
                new LimitOperation(
                        user,
                        reservation,
                        LimitOperationType.RESERVE,
                        amount.negate(),
                        null
                )
        );

        return reservation;
    }
}
