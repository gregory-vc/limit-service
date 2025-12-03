package limit.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "limit_reservations")
public class LimitReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private LimitReservationStatus status;

    @Column(name = "request_id", nullable = false, length = 128, unique = true)
    private String requestId;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    protected LimitReservation() {
    }

    public LimitReservation(User user,
                            BigDecimal amount,
                            LimitReservationStatus status,
                            String requestId,
                            OffsetDateTime expiresAt,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt) {
        this.user = user;
        this.amount = amount;
        this.status = status;
        this.requestId = requestId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LimitReservationStatus getStatus() {
        return status;
    }

    public void setStatus(LimitReservationStatus status) {
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
