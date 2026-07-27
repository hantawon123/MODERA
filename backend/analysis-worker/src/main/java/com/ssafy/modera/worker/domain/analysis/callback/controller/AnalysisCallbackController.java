package com.ssafy.modera.worker.domain.analysis.callback.controller;

import com.ssafy.modera.worker.domain.analysis.callback.dto.request.AnalysisCallbackRequest;
import com.ssafy.modera.worker.domain.analysis.callback.service.AnalysisCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class AnalysisCallbackController {

    private final AnalysisCallbackService analysisCallbackService;

    @Value("${internal.callback.token}")
    private String expectedToken;

    @PostMapping("/callback/analysis")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody AnalysisCallbackRequest request
    ) {
        verifyToken(token);

        log.info("분석 콜백 수신: jobId={} imageId={} stage={} status={} resultKeys={}",
                request.jobId(), request.imageId(), request.stage(), request.status(),
                request.result() == null ? "none" : request.result().keySet());

        analysisCallbackService.handle(request);

        return ResponseEntity.ok(Map.of("received", true));
    }

    private void verifyToken(String token) {
        if (token == null || !token.equals(expectedToken)) {
            log.warn("분석 콜백 토큰 불일치 — 요청 거부");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}