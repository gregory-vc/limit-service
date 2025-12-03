package limit.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_limits")
public class UserLimit {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "available_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableLimit;

    @Column(name = "reserved_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedAmount;

    @Column(name = "last_reset_at", nullable = false)
    private OffsetDateTime lastResetAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected UserLimit() {
    }

    public UserLimit(User user, BigDecimal availableLimit, BigDecimal reservedAmount, OffsetDateTime lastResetAt, OffsetDateTime updatedAt) {
        this.user = user;
        this.userId = user.getId();
        this.availableLimit = availableLimit;
        this.reservedAmount = reservedAmount;
        this.lastResetAt = lastResetAt;
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getAvailableLimit() {
        return availableLimit;
    }

    public void setAvailableLimit(BigDecimal availableLimit) {
        this.availableLimit = availableLimit;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public OffsetDateTime getLastResetAt() {
        return lastResetAt;
    }

    public void setLastResetAt(OffsetDateTime lastResetAt) {
        this.lastResetAt = lastResetAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
