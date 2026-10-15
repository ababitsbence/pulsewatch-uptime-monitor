package com.pulsewatch.monitor;

import com.pulsewatch.common.RequestValidationException;
import com.pulsewatch.common.ResourceNotFoundException;
import com.pulsewatch.ssrf.SsrfGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class MonitorService {

    private static final int DEFAULT_EXPECTED_STATUS = 200;

    private final MonitorRepository monitorRepository;
    private final SsrfGuard ssrfGuard;

    public MonitorService(MonitorRepository monitorRepository, SsrfGuard ssrfGuard) {
        this.monitorRepository = monitorRepository;
        this.ssrfGuard = ssrfGuard;
    }

    @Transactional
    public MonitorResponse create(Long userId, MonitorRequest request) {
        validate(request);
        Monitor monitor = new Monitor(
                userId,
                request.name().trim(),
                request.target().trim(),
                request.type(),
                request.intervalSeconds(),
                request.timeoutSeconds(),
                expectedStatusFor(request),
                isEnabled(request));
        return MonitorResponse.from(monitorRepository.save(monitor));
    }

    @Transactional(readOnly = true)
    public List<MonitorResponse> list(Long userId) {
        return monitorRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(MonitorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitorResponse get(Long userId, Long id) {
        return MonitorResponse.from(findOwned(userId, id));
    }

    @Transactional
    public MonitorResponse update(Long userId, Long id, MonitorRequest request) {
        validate(request);
        Monitor monitor = findOwned(userId, id);
        monitor.update(
                request.name().trim(),
                request.target().trim(),
                request.type(),
                request.intervalSeconds(),
                request.timeoutSeconds(),
                expectedStatusFor(request),
                isEnabled(request));
        return MonitorResponse.from(monitor);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        monitorRepository.delete(findOwned(userId, id));
    }

    private void validate(MonitorRequest request) {
        MonitorRequestValidator.validate(request);
        TargetHost.of(request.type(), request.target())
                .flatMap(ssrfGuard::violation)
                .ifPresent(message -> {
                    throw new RequestValidationException(Map.of("target", message));
                });
    }

    // Another user's monitor looks exactly like one that does not exist.
    private Monitor findOwned(Long userId, Long id) {
        return monitorRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
    }

    private static Integer expectedStatusFor(MonitorRequest request) {
        if (request.type() != MonitorType.HTTP) {
            return null;
        }
        return request.expectedStatus() != null ? request.expectedStatus() : DEFAULT_EXPECTED_STATUS;
    }

    private static boolean isEnabled(MonitorRequest request) {
        return !Boolean.FALSE.equals(request.enabled());
    }
}