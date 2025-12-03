package limit.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "limit_operations")
public class LimitOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private LimitReservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 16)
    private LimitOperationType operationType;

    @Column(name = "change_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LimitOperation() {
    }

    public LimitOperation(User user,
                          LimitReservation reservation,
                          LimitOperationType operationType,
                          BigDecimal changeAmount,
                          OffsetDateTime createdAt) {
        this.user = user;
        this.reservation = reservation;
        this.operationType = operationType;
        this.changeAmount = changeAmount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LimitReservation getReservation() {
        return reservation;
    }

    public LimitOperationType getOperationType() {
        return operationType;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
