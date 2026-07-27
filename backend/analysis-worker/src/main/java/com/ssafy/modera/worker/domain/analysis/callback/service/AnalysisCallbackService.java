package com.ssafy.modera.worker.domain.analysis.callback.service;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import com.ssafy.modera.worker.domain.analysis.callback.dto.request.AnalysisCallbackRequest;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisJobRepository;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisResultRepository;
import com.ssafy.modera.worker.domain.analysis.repository.AnalysisResultRow;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCallbackService {

    private static final int EMBEDDING_DIM = 768;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void handle(AnalysisCallbackRequest request) {
        AnalysisJob analysisJob = analysisJobRepository.findById(request.jobId()).orElse(null);
        if (analysisJob == null) {
            // 영구 오류: 재시도 해도 없음. 로그만 남기고 받아들임.
            log.warn("콜백 대상 job 없음 -> 무시: jobId={} imageId={}", request.jobId(), request.imageId());
            return;
        }

        // 멱등: AI가 최대 3회 재전송하므로 이미 끝난 job이면 무시
        if (isFinished(analysisJob)) {
            log.info("이미 처리된 job - 콜백 무시: jobId={}, status={}", request.jobId(), analysisJob.getStatus());
            return;
        }

        switch (request.status()) {
            case "COMPLETED", "EMPTY" -> handleSuccess(analysisJob, request);
            case "FAILED" -> handleFailure(analysisJob, request);
            default -> log.warn("알 수 없는 콜백 status — 무시: jobId={} status={}", request.jobId(), request.status());
        }
    }
    private boolean isFinished(AnalysisJob job) {
        return "COMPLETED".equals(job.getStatus()) || "FAILED".equals(job.getStatus());
    }

    private void handleSuccess(AnalysisJob analysisJob, AnalysisCallbackRequest request) {
        Map<String, Object> result = request.result() == null ? Map.of() : request.result();

        boolean inserted = analysisResultRepository.insert(new AnalysisResultRow(
                analysisJob.getJobId(),
                analysisJob.getImageId(),
                null,                         // ocrRawText — 콜백에 없음(추후 결정)
                str(result, "ocrRefinedText"),
                null,                            // ocrLang
                null,                                    // ocrConfidence
                str(result, "summary"),
                bool(result, "informative"),
                null,                      // structuredType
                null,                                   // structuredFieldsJson
                null,                                   // keyInformationJson
                flt(result, "analysisConfidence"),
                vector(result, "documentVector"),
                request.modelVersion(),
                OffsetDateTime.now().toInstant()
        ));
        if (!inserted) {
            log.info("이미 저장된 (imageId, modelVersion) — 결과 저장 건너뜀: imageId={} modelVersion={}",
                    analysisJob.getImageId(), request.modelVersion());
        }

        analysisJob.markCompleted(request.modelVersion(), OffsetDateTime.now());
        analysisJobRepository.save(analysisJob);

        // analysisStatus는 AI가 보낸 값을 그대로 넘긴다.
        // EMPTY(OCR이 비었거나 비정보성이라 분석을 생략함)를 COMPLETED로 덮어쓰면
        // api-server의 user_image.analysis_status가 "정상 분석 완료"로 기록돼
        // 분석을 건너뛴 이미지와 실제로 분석된 이미지를 구분할 수 없다.
        AnalysisCompletedPayload payload = new AnalysisCompletedPayload(
                analysisJob.getImageId(), analysisJob.getUserId(),
                str(result, "summary"), str(result, "ocrRefinedText"),
                null, null, null, request.status(), request.modelVersion()
        );
        eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_COMPLETED, 1, payload);
        log.info("ANALYSIS_COMPLETED 발행: jobId={} imageId={}", analysisJob.getJobId(), analysisJob.getImageId());
    }

    private void handleFailure(AnalysisJob job, AnalysisCallbackRequest req) {
        String code = req.error() == null ? "ANALYSIS_ERROR" : req.error().code();
        String message = req.error() == null ? "unknown" : req.error().message();
        boolean retryable = req.error() != null && Boolean.TRUE.equals(req.error().retryable());

        job.markFailed(code, message, retryable, OffsetDateTime.now());
        analysisJobRepository.save(job);

        AnalysisFailedPayload payload = new AnalysisFailedPayload(
                job.getImageId(), job.getUserId(), code, message, retryable);
        eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_FAILED, 1, payload);
        log.warn("ANALYSIS_FAILED 발행: jobId={} code={}", job.getJobId(), code);
    }

    // ── result(Map) 접근 헬퍼 ──
    // AI 콜백의 result는 stage마다 필드가 달라 Map으로 받는다.
    // 키가 없거나 타입이 다르면 null을 돌려주고, 저장은 그대로 진행한다(부분 결과 허용).

    private String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private Boolean bool(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Boolean b ? b : null;
    }

    private Double dbl(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private float[] vector(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        // DB 컬럼이 vector(768)이라 차원이 다르면 INSERT가 깨진다. 미리 걸러 원인을 명확히 남긴다.
        if (list.size() != EMBEDDING_DIM) {
            log.warn("임베딩 차원 불일치 — 저장 생략: expected={} actual={}", EMBEDDING_DIM, list.size());
            return null;
        }
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i) instanceof Number n)) {
                log.warn("임베딩에 숫자가 아닌 값 — 저장 생략: index={}", i);
                return null;
            }
            result[i] = n.floatValue();
        }
        return result;
    }

    private Float flt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.floatValue() : null;
    }
}
