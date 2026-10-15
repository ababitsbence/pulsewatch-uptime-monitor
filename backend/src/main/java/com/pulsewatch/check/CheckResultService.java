package com.pulsewatch.check;

import com.pulsewatch.common.RequestValidationException;
import com.pulsewatch.common.ResourceNotFoundException;
import com.pulsewatch.monitor.MonitorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@Service
public class CheckResultService {

    static final int MAX_LIMIT = 200;

    private final CheckResultRepository checkResultRepository;
    private final MonitorRepository monitorRepository;
    private final Clock clock;

    public CheckResultService(CheckResultRepository checkResultRepository,
                              MonitorRepository monitorRepository,
                              Clock clock) {
        this.checkResultRepository = checkResultRepository;
        this.monitorRepository = monitorRepository;
        this.clock = clock;
    }

    @Transactional
    public void record(Long monitorId, CheckOutcome outcome) {
        checkResultRepository.save(new CheckResult(
                monitorId,
                clock.instant(),
                outcome.status(),
                outcome.responseTimeMs(),
                outcome.error()));
    }

    @Transactional(readOnly = true)
    public List<CheckResultResponse> history(Long userId, Long monitorId, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new RequestValidationException(
                    Map.of("limit", "must be between 1 and " + MAX_LIMIT));
        }
        monitorRepository.findByIdAndUserId(monitorId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
        return checkResultRepository
                .findByMonitorIdOrderByCheckedAtDesc(monitorId, PageRequest.of(0, limit))
                .stream()
                .map(CheckResultResponse::from)
                .toList();
    }
}