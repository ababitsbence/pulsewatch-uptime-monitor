package com.pulsewatch.monitor;

import com.pulsewatch.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    public List<MonitorResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return monitorService.list(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitorResponse create(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody MonitorRequest request) {
        return monitorService.create(CurrentUser.id(jwt), request);
    }

    @GetMapping("/{id}")
    public MonitorResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return monitorService.get(CurrentUser.id(jwt), id);
    }

    @PutMapping("/{id}")
    public MonitorResponse update(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable Long id,
                                  @Valid @RequestBody MonitorRequest request) {
        return monitorService.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        monitorService.delete(CurrentUser.id(jwt), id);
    }
}