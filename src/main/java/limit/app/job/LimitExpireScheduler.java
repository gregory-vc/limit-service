package limit.app.job;

import limit.app.config.LimitServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
public class LimitExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(LimitExpireScheduler.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LimitExpireScheduler(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${limit.expire-cron}")
    public void expireReservations() {
        OffsetDateTime now = OffsetDateTime.now();

        var params = new MapSqlParameterSource()
                .addValue("now", now);

        var stats = jdbcTemplate.queryForMap(
                """
                WITH expired AS (
                    SELECT id, user_id, amount
                    FROM limit_reservations
                    WHERE status = 'RESERVED' AND expires_at IS NOT NULL AND expires_at <= :now
                ),
                updated_limits AS (
                    UPDATE user_limits ul
                    SET reserved_amount = reserved_amount - e.amount,
                        available_limit = available_limit + e.amount,
                        updated_at = :now
                    FROM expired e
                    WHERE ul.user_id = e.user_id
                    RETURNING e.id AS reservation_id, e.user_id, e.amount
                ),
                updated_reservations AS (
                    UPDATE limit_reservations lr
                    SET status = 'RELEASED',
                        updated_at = :now
                    FROM expired e
                    WHERE lr.id = e.id
                    RETURNING lr.id
                ),
                inserted_ops AS (
                    INSERT INTO limit_operations (user_id, reservation_id, operation_type, change_amount, created_at)
                    SELECT e.user_id, e.reservation_id, 'RELEASE', e.amount, :now
                    FROM updated_limits e
                    RETURNING 1
                )
                SELECT
                    (SELECT count(*) FROM expired) AS expired_count,
                    (SELECT count(*) FROM updated_limits) AS limits_updated,
                    (SELECT count(*) FROM updated_reservations) AS reservations_updated,
                    (SELECT count(*) FROM inserted_ops) AS operations_inserted
                """,
                params
        );

        long expired = ((Number) stats.getOrDefault("expired_count", 0)).longValue();
        long updatedLimits = ((Number) stats.getOrDefault("limits_updated", 0)).longValue();
        long updatedRes = ((Number) stats.getOrDefault("reservations_updated", 0)).longValue();
        long ops = ((Number) stats.getOrDefault("operations_inserted", 0)).longValue();

        if (expired > 0) {
            log.info("Expired reservations processed: {}, limits updated {}, reservations {}, operations {}", expired, updatedLimits, updatedRes, ops);
        } else {
            log.debug("No expired reservations at {}", now);
        }
    }
}
