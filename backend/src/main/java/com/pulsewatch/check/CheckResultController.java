package com.pulsewatch.check;

import com.pulsewatch.auth.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitors/{monitorId}/results")
public class CheckResultController {

    private final CheckResultService checkResultService;

    public CheckResultController(CheckResultService checkResultService) {
        this.checkResultService = checkResultService;
    }

    @GetMapping
    public List<CheckResultResponse> history(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable Long monitorId,
                                             @RequestParam(defaultValue = "50") int limit) {
        return checkResultService.history(CurrentUser.id(jwt), monitorId, limit);
    }
}