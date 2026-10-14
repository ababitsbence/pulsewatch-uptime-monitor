package com.pulsewatch.monitor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Monitor> findByIdAndUserId(Long id, Long userId);
}