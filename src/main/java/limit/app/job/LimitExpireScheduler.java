package limit.app.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LimitExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(LimitExpireScheduler.class);
    private static final long EXPIRE_LOCK_ID = 0x4C494D4954455850L;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LimitExpireScheduler(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${limit.expire-cron}")
    public void expireReservations() {
        jdbcTemplate.getJdbcTemplate().execute((ConnectionCallback<Void>) connection -> {
            var connectionTemplate = new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true));

            boolean locked = tryAcquireLock(connectionTemplate);
            if (!locked) {
                log.warn("Skip expire job start: another instance is already running");
                return null;
            }

            try {
                log.info("Expire reservations job started");
                var stats = connectionTemplate.queryForMap(
                        """
                        WITH expired AS (
                            SELECT id, user_id, amount
                            FROM limit_reservations
                            WHERE status = 'RESERVED' AND expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP
                            FOR UPDATE SKIP LOCKED
                        ),
                        updated_limits AS (
                            UPDATE user_limits ul
                            SET reserved_amount = reserved_amount - e.amount,
                                available_limit = available_limit + e.amount
                            FROM expired e
                            WHERE ul.user_id = e.user_id
                            RETURNING e.id AS reservation_id, e.user_id, e.amount
                        ),
                        updated_reservations AS (
                            UPDATE limit_reservations lr
                            SET status = 'RELEASED'
                            FROM expired e
                            WHERE lr.id = e.id
                            RETURNING lr.id
                        ),
                        inserted_ops AS (
                            INSERT INTO limit_operations (user_id, reservation_id, operation_type, change_amount)
                            SELECT e.user_id, e.reservation_id, 'RELEASE', e.amount
                            FROM updated_limits e
                            RETURNING 1
                        )
                        SELECT
                            (SELECT count(*) FROM expired) AS expired_count,
                            (SELECT count(*) FROM updated_limits) AS limits_updated,
                            (SELECT count(*) FROM updated_reservations) AS reservations_updated,
                            (SELECT count(*) FROM inserted_ops) AS operations_inserted
                        """,
                        new MapSqlParameterSource()
                );

                long expired = ((Number) stats.getOrDefault("expired_count", 0)).longValue();
                long updatedLimits = ((Number) stats.getOrDefault("limits_updated", 0)).longValue();
                long updatedRes = ((Number) stats.getOrDefault("reservations_updated", 0)).longValue();
                long ops = ((Number) stats.getOrDefault("operations_inserted", 0)).longValue();

                if (expired > 0) {
                    log.info("Expired reservations processed: {}, limits updated {}, reservations {}, operations {}", expired, updatedLimits, updatedRes, ops);
                } else {
                    log.info("No expired reservations at CURRENT_TIMESTAMP");
                }
            } finally {
                releaseLock(connectionTemplate);
            }
            return null;
        });
    }

    private boolean tryAcquireLock(NamedParameterJdbcTemplate template) {
        var params = new MapSqlParameterSource().addValue("id", LimitExpireScheduler.EXPIRE_LOCK_ID);
        Boolean locked = template.queryForObject("SELECT pg_try_advisory_lock(:id)", params, Boolean.class);
        return Boolean.TRUE.equals(locked);
    }

    private void releaseLock(NamedParameterJdbcTemplate template) {
        var params = new MapSqlParameterSource().addValue("id", LimitExpireScheduler.EXPIRE_LOCK_ID);
        template.queryForObject("SELECT pg_advisory_unlock(:id)", params, Boolean.class);
    }
}
