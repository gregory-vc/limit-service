package limit.app.job;

import limit.app.config.LimitResetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class LimitResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(LimitResetScheduler.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final LimitResetProperties properties;

    public LimitResetScheduler(NamedParameterJdbcTemplate jdbcTemplate, LimitResetProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Scheduled(cron = "${limit.reset-cron:0 0 0 * * *}")
    public void resetAllLimits() {
        BigDecimal defaultLimit = properties.getDefaultValue();
        OffsetDateTime now = OffsetDateTime.now();

        final int batchSize = properties.getResetBatchSize();
        long updatedTotal = 0;
        long lastId = 0;
        int batchNumber = 0;

        while (true) {
            var params = new MapSqlParameterSource()
                    .addValue("defaultLimit", defaultLimit)
                    .addValue("now", now)
                    .addValue("lastId", lastId)
                    .addValue("batchSize", batchSize);

            var stats = jdbcTemplate.queryForMap(
                    """
                    WITH target AS (
                        SELECT user_id, available_limit, reserved_amount
                        FROM user_limits
                        WHERE user_id > :lastId
                          AND available_limit <> GREATEST(:defaultLimit - reserved_amount, 0)
                        ORDER BY user_id
                        LIMIT :batchSize
                    ),
                    updated AS (
                        UPDATE user_limits ul
                        SET available_limit = GREATEST(:defaultLimit - ul.reserved_amount, 0),
                            last_reset_at = :now,
                            updated_at = :now
                        FROM target t
                        WHERE ul.user_id = t.user_id
                          AND t.available_limit <> GREATEST(:defaultLimit - ul.reserved_amount, 0)
                        RETURNING ul.user_id, t.available_limit AS old_available, ul.available_limit AS new_available
                    ),
                    inserted AS (
                        INSERT INTO limit_operations (user_id, operation_type, status, change_amount, description, created_at)
                        SELECT user_id,
                               'RESET',
                               'APPLIED',
                               (new_available - old_available),
                               'Daily reset to default limit (reserved kept)',
                               :now
                        FROM updated
                        RETURNING 1
                    )
                    SELECT
                        (SELECT count(*) FROM target)   AS batch_size,
                        (SELECT count(*) FROM updated)  AS updated_users,
                        (SELECT max(user_id) FROM target) AS max_user_id
                    """,
                    params
            );

            long batchTaken = ((Number) stats.getOrDefault("batch_size", 0)).longValue();
            if (batchTaken == 0) {
                break;
            }

            long updated = ((Number) stats.getOrDefault("updated_users", 0)).longValue();
            long maxUserId = stats.get("max_user_id") == null ? lastId : ((Number) stats.get("max_user_id")).longValue();

            updatedTotal += updated;
            batchNumber++;

            log.info(
                    "Limit reset batch {}: taken {}, updated {} (total updated {})",
                    batchNumber,
                    batchTaken,
                    updated,
                    updatedTotal
            );

            lastId = maxUserId;
        }

        log.info(
                "Limit reset job finished: updated {}, default limit {}",
                updatedTotal,
                defaultLimit
        );
    }
}
