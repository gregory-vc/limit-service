package limit.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "limit")
public class LimitResetProperties {

    private BigDecimal defaultValue;

    private String resetCron;

    private int resetBatchSize;

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
}
