package limit.app.job;

import limit.app.config.LimitServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

@Component
public class LimitResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(LimitResetScheduler.class);
    private static final long RESET_LOCK_ID = 0x4C494D4954524553L;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final LimitServiceProperties properties;

    public LimitResetScheduler(NamedParameterJdbcTemplate jdbcTemplate, LimitServiceProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Scheduled(cron = "${limit.reset-cron}")
    public void resetAllLimits() {
        jdbcTemplate.getJdbcTemplate().execute((ConnectionCallback<Void>) connection -> {
            var connectionTemplate = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));

            boolean locked = tryAcquireLock(connectionTemplate);
            if (!locked) {
                log.warn("Skip reset job start: another instance is already running");
                return null;
            }

            try {
                BigDecimal defaultLimit = properties.getDefaultValue();
                String runToken = UUID.randomUUID().toString();
                Duration waitDuration = properties.getResetWait();

                final int batchSize = properties.getResetBatchSize();
                long updatedTotal = 0;
                int batchNumber = 0;

                while (true) {
                    var params = new MapSqlParameterSource()
                            .addValue("defaultLimit", defaultLimit)
                            .addValue("runToken", runToken)
                            .addValue("batchSize", batchSize);

                    var stats = connectionTemplate.queryForMap(
                            """
                            WITH target AS (
                                SELECT user_id, available_limit
                                FROM user_limits
                                WHERE last_reset_token IS DISTINCT FROM :runToken
                                  AND available_limit <> GREATEST(:defaultLimit - reserved_amount, 0)
                                ORDER BY user_id
                                LIMIT :batchSize
                                FOR UPDATE SKIP LOCKED
                            ),
                            updated AS (
                                UPDATE user_limits ul
                                SET available_limit = GREATEST(:defaultLimit - ul.reserved_amount, 0),
                                    last_reset_token = :runToken
                                FROM target t
                                WHERE ul.user_id = t.user_id
                                RETURNING ul.user_id, t.available_limit AS old_available, ul.available_limit AS new_available
                            ),
                            inserted AS (
                                INSERT INTO limit_operations (user_id, operation_type, change_amount)
                                SELECT user_id,
                                       'RESET',
                                       (new_available - old_available)
                                FROM updated
                                WHERE new_available <> old_available
                                RETURNING 1
                            )
                            SELECT
                                (SELECT count(*) FROM target)   AS batch_size,
                                (SELECT count(*) FROM updated)  AS updated_users,
                                (SELECT count(*) FROM inserted) AS operations_inserted
                            """,
                            params
                    );

                    long batchTaken = ((Number) stats.getOrDefault("batch_size", 0)).longValue();
                    if (batchTaken == 0) {
                        Long remaining = connectionTemplate.queryForObject(
                                """
                                SELECT count(*) FROM user_limits
                                WHERE last_reset_token IS DISTINCT FROM :runToken
                                  AND available_limit <> GREATEST(:defaultLimit - reserved_amount, 0)
                                """,
                                params,
                                Long.class
                        );

                        if (remaining != null && remaining > 0) {
                            LockSupport.parkNanos(waitDuration.toNanos());
                            if (Thread.currentThread().isInterrupted()) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            continue;
                        } else {
                            break;
                        }
                    }

                    long updated = ((Number) stats.getOrDefault("updated_users", 0)).longValue();
                    long ops = ((Number) stats.getOrDefault("operations_inserted", 0)).longValue();

                    updatedTotal += updated;
                    batchNumber++;

                    log.info(
                            "Limit reset batch {}: taken {}, updated {}, ops {} (total updated {})",
                            batchNumber,
                            batchTaken,
                            updated,
                            ops,
                            updatedTotal
                    );
                }

                log.info(
                        "Limit reset job finished: updated {}, default limit {}, run token {}",
                        updatedTotal,
                        defaultLimit,
                        runToken
                );
            } finally {
                releaseLock(connectionTemplate);
            }
            return null;
        });
    }

    private boolean tryAcquireLock(NamedParameterJdbcTemplate template) {
        var params = new MapSqlParameterSource().addValue("id", LimitResetScheduler.RESET_LOCK_ID);
        Boolean locked = template.queryForObject("SELECT pg_try_advisory_lock(:id)", params, Boolean.class);
        return Boolean.TRUE.equals(locked);
    }

    private void releaseLock(NamedParameterJdbcTemplate template) {
        var params = new MapSqlParameterSource().addValue("id", LimitResetScheduler.RESET_LOCK_ID);
        template.queryForObject("SELECT pg_advisory_unlock(:id)", params, Boolean.class);
    }
}
