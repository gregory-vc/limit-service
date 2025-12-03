package limit.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "limit")
public class LimitServiceProperties {

    private BigDecimal defaultValue;

    private String resetCron;

    private int resetBatchSize;

    private Duration resetWait;

    private Duration reservationTtl;

    private String expireCron;

    public BigDecimal getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(BigDecimal defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getResetCron() {
        return resetCron;
    }

    public void setResetCron(String resetCron) {
        this.resetCron = resetCron;
    }

    public int getResetBatchSize() {
        return resetBatchSize;
    }

    public void setResetBatchSize(int resetBatchSize) {
        this.resetBatchSize = resetBatchSize;
    }

    public Duration getResetWait() {
        return resetWait;
    }

    public void setResetWait(Duration resetWait) {
        this.resetWait = resetWait;
    }

    public Duration getReservationTtl() {
        return reservationTtl;
    }

    public void setReservationTtl(Duration reservationTtl) {
        this.reservationTtl = reservationTtl;
    }
}
