package com.pulsewatch.monitor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Monitor> findByIdAndUserId(Long id, Long userId);

    @Query(value = """
            SELECT m.* FROM monitors m
            WHERE m.enabled = TRUE
              AND NOT EXISTS (
                  SELECT 1 FROM check_results r
                  WHERE r.monitor_id = m.id
                    AND r.checked_at > CAST(:now AS timestamptz) - m.interval_seconds * INTERVAL '1 second')
            """, nativeQuery = true)
    List<Monitor> findDueMonitors(@Param("now") Instant now);
}