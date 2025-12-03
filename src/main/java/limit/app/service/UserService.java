package limit.app.service;

import limit.app.config.LimitServiceProperties;
import limit.app.exception.*;
import limit.app.repository.LimitOperationRepository;
import limit.app.repository.LimitReservationRepository;
import limit.app.repository.UserLimitRepository;
import limit.app.repository.UserRepository;
import limit.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserLimitRepository userLimitRepository;
    private final LimitReservationRepository reservationRepository;
    private final LimitOperationRepository operationRepository;
    private final LimitServiceProperties properties;

    public UserService(UserRepository userRepository,
                       UserLimitRepository userLimitRepository,
                       LimitReservationRepository reservationRepository,
                       LimitOperationRepository operationRepository,
                       LimitServiceProperties properties) {
        this.userRepository = userRepository;
        this.userLimitRepository = userLimitRepository;
        this.reservationRepository = reservationRepository;
        this.operationRepository = operationRepository;
        this.properties = properties;
    }

    @Transactional
    public LimitReservation reserveLimit(String externalUserId, BigDecimal amount, String requestId) {
        validateAmount(amount);
        if (requestId == null || requestId.isBlank()) {
            throw new InvalidRequestException("requestId must be provided");
        }

        var existing = reservationRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return existing.get();
        }

        var user = ensureUser(externalUserId);
        var userLimit = ensureUserLimit(user);

        BigDecimal newAvailable = userLimit.getAvailableLimit().subtract(amount);
        if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientLimitException("Insufficient available limit for reservation");
        }

        userLimit.setAvailableLimit(newAvailable);
        userLimit.setReservedAmount(userLimit.getReservedAmount().add(amount));
        userLimitRepository.save(userLimit);

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(properties.getReservationTtl());

        var reservation = reservationRepository.save(
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

    @Transactional
    public LimitReservation confirmLimitAndDebit(Long reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation %d not found".formatted(reservationId)));

        if (reservation.getStatus() != LimitReservationStatus.RESERVED) {
            throw new ReservationInvalidStateException("Reservation %d is not in RESERVED status".formatted(reservationId));
        }

        var userLimit = userLimitRepository.findById(reservation.getUser().getId())
                .orElseThrow(() -> new UserLimitNotFoundException("User limit not found for user " + reservation.getUser().getId()));

        if (userLimit.getReservedAmount().compareTo(reservation.getAmount()) < 0) {
            throw new ReservationInvalidStateException("Reserved amount is insufficient to confirm reservation " + reservationId);
        }

        userLimit.setReservedAmount(userLimit.getReservedAmount().subtract(reservation.getAmount()));
        userLimitRepository.save(userLimit);

        reservation.setStatus(LimitReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

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
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation %d not found".formatted(reservationId)));

        if (reservation.getStatus() != LimitReservationStatus.RESERVED) {
            throw new ReservationInvalidStateException("Reservation %d cannot be cancelled from status %s".formatted(
                    reservationId, reservation.getStatus()));
        }

        var userLimit = userLimitRepository.findById(reservation.getUser().getId())
                .orElseThrow(() -> new UserLimitNotFoundException("User limit not found for user " + reservation.getUser().getId()));

        userLimit.setReservedAmount(userLimit.getReservedAmount().subtract(reservation.getAmount()));
        userLimit.setAvailableLimit(userLimit.getAvailableLimit().add(reservation.getAmount()));
        userLimitRepository.save(userLimit);

        reservation.setStatus(LimitReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

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
        return userRepository.findByExternalId(externalUserId)
                .orElseGet(() -> userRepository.save(new User(externalUserId, null, null)));
    }

    private UserLimit ensureUserLimit(User user) {
        return userLimitRepository.findById(user.getId())
                .orElseGet(() -> userLimitRepository.save(
                        new UserLimit(
                                user,
                                properties.getDefaultValue(),
                                BigDecimal.ZERO,
                                null,
                                null
                        )
                ));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Amount must be positive");
        }
    }
}
