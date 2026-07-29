package com.ssafy.modera.worker.domain.analysis.callback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCallbackService {

    private static final int EMBEDDING_DIM = 768;
    /** 콜백은 정상 도착했지만 worker가 결과를 남기지 못한 경우. AI 분석 자체의 실패와 구분한다. */
    private static final String CALLBACK_PERSIST_ERROR = "CALLBACK_PERSIST_ERROR";
    private static final int MAX_ERROR_MESSAGE = 500;

    private final AnalysisJobRepository analysisJobRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final EventPublisher eventPublisher;
    // JacksonConfig가 등록한 Jackson 2 ObjectMapper. jsonb 컬럼용 직렬화에 쓴다.
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 콜백 처리 진입점.
     *
     * 결과 저장·이벤트 발행이 깨지면 트랜잭션이 롤백되면서 job이 PROCESSING에 그대로 남고
     * ANALYSIS_FAILED도 나가지 않는다 — AI가 3회 재시도하고 포기하면 아무도 모르는 채로
     * 영구 정지한다. 리팩터링 이전 동기 방식은 같은 상황에서 job을 FAILED로 남겼다.
     * 그 관측성을 되돌리기 위해 예외를 잡아 별도 트랜잭션으로 실패를 확정한다.
     *
     * 트랜잭션 경계를 @Transactional 대신 TransactionTemplate으로 직접 다루는 이유:
     * "실패를 기록하는 트랜잭션"은 롤백된 트랜잭션과 별개여야 하는데, 같은 빈 안에서
     * self-invocation을 하면 프록시를 타지 않아 @Transactional이 적용되지 않는다.
     */
    public void handle(AnalysisCallbackRequest request) {
        try {
            transactionTemplate.executeWithoutResult(status -> apply(request));
        } catch (Exception e) {
            markCallbackFailed(request, e);
        }
    }

    /**
     * 콜백 처리가 깨졌을 때 job을 FAILED로 확정하고 ANALYSIS_FAILED를 발행한다.
     *
     * AI에는 정상 응답(200)을 돌려주게 되므로 재시도가 멈춘다. AI의 재시도 창은 백오프를
     * 포함해도 약 3초라 실제 DB 장애가 그 안에 낫는 경우는 드물고, job을 FAILED로 찍는
     * 순간 isFinished()가 true가 되어 어차피 재시도가 무시된다 — 둘은 양립하지 않는다.
     * retryable=true로 남겨 두어 나중에 재분석 대상으로 고를 수 있게 한다.
     *
     * 이 기록마저 실패하면(DB가 통째로 죽은 경우) 남은 것이 없으므로 예외를 그대로 올려
     * 500을 내보낸다. 이때는 AI 재시도가 유일한 복구 수단이다.
     */
    private void markCallbackFailed(AnalysisCallbackRequest request, Exception cause) {
        log.error("콜백 처리 실패 — job을 FAILED로 확정한다: jobId={} imageId={}",
                request.jobId(), request.imageId(), cause);
        transactionTemplate.executeWithoutResult(status -> {
            AnalysisJob job = analysisJobRepository.findById(request.jobId()).orElse(null);
            if (job == null || isFinished(job)) {
                return;
            }
            String message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            job.markFailed(CALLBACK_PERSIST_ERROR, truncate(message), true, OffsetDateTime.now());
            analysisJobRepository.save(job);

            if (!hasUserId(job, EventTypes.ANALYSIS_FAILED)) {
                return;
            }
            eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.ANALYSIS_FAILED, 1,
                    new AnalysisFailedPayload(job.getImageId(), job.getUserId(),
                            CALLBACK_PERSIST_ERROR, truncate(message), true));
            log.warn("ANALYSIS_FAILED 발행(콜백 처리 실패): jobId={}", job.getJobId());
        });
    }

    /** error_message는 TEXT지만 원인 예외 전문이 그대로 들어가면 로그·조회가 지저분해진다. */
    private String truncate(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.length() <= MAX_ERROR_MESSAGE ? message : message.substring(0, MAX_ERROR_MESSAGE);
    }

    private void apply(AnalysisCallbackRequest request) {
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

    /**
     * 결과 이벤트를 발행해도 되는지 확인한다. userId가 없으면 발행하지 않는다.
     *
     * analysis_job.user_id는 마이그레이션 003에서 default 없이 추가돼 그 이전 행은 전부
     * NULL이다. 반면 AnalysisCompletedPayload·AnalysisFailedPayload의 userId는 primitive
     * int라 그대로 넘기면 언박싱 NPE가 난다.
     *
     * 0 같은 대체값을 넣지 않는 이유: api-server가 이 값을 그대로 query_schema의
     * user_image_view.user_id로 쓴다. 틀린 userId가 들어가면 read model이 조용히
     * 오염되고, 그건 "이벤트를 재생하면 재구축할 수 있다"는 read model의 전제를 깬다.
     * 이벤트를 아예 안 보내면 user_id를 채운 뒤 재발행해 복구할 수 있다.
     *
     * job 상태와 분석 결과 저장은 이미 끝난 뒤라 그대로 남는다 — 버리는 건 이벤트뿐이다.
     */
    private boolean hasUserId(AnalysisJob job, String eventType) {
        if (job.getUserId() != null) {
            return true;
        }
        log.error("job에 userId가 없어 {} 발행을 생략한다(003 마이그레이션 이전 행으로 추정). "
                        + "user_id를 채운 뒤 재발행이 필요하다: jobId={} imageId={}",
                eventType, job.getJobId(), job.getImageId());
        return false;
    }

    private void handleSuccess(AnalysisJob analysisJob, AnalysisCallbackRequest request) {
        Map<String, Object> result = request.result() == null ? Map.of() : request.result();

        // AI는 이미지 유형에 따라 다른 키로 구조화 데이터를 보낸다(일정이면 scheduleData,
        // 상품이면 structuredData). 둘 다 { type, fields } 형태라 같은 방식으로 푼다.
        Map<String, Object> structured = structuredData(result);

        boolean inserted = analysisResultRepository.insert(new AnalysisResultRow(
                analysisJob.getJobId(),
                analysisJob.getImageId(),
                // AI 콜백은 refined 텍스트만 돌려준다. raw/lang/confidence는 앱이 보낸
                // 온디바이스 OCR이 유일한 출처라, job 생성 시 실어둔 값을 여기서 꺼내 쓴다.
                analysisJob.getClientOcrRawText(),
                str(result, "ocrRefinedText"),
                analysisJob.getClientOcrLang(),
                analysisJob.getClientOcrConfidence(),
                str(result, "summary"),
                bool(result, "informative"),
                str(structured, "type"),
                jsonObject(structured, "fields"),
                jsonArray(result, "keyInformation"),
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

        if (!hasUserId(analysisJob, EventTypes.ANALYSIS_COMPLETED)) {
            return;
        }

        // analysisStatus는 AI가 보낸 값을 그대로 넘긴다.
        // EMPTY(OCR이 비었거나 비정보성이라 분석을 생략함)를 COMPLETED로 덮어쓰면
        // api-server의 user_image.analysis_status가 "정상 분석 완료"로 기록돼
        // 분석을 건너뛴 이미지와 실제로 분석된 이미지를 구분할 수 없다.
        AnalysisCompletedPayload payload = new AnalysisCompletedPayload(
                analysisJob.getImageId(), analysisJob.getUserId(),
                str(result, "title"),
                str(result, "summary"),
                str(result, "ocrRefinedText"),
                str(result, "thumbnailKey"),
                str(result, "category"),
                strList(result, "tags"),
                strList(result, "keyInformation"),
                jsonObject(structured, "fields"),
                request.status(),
                request.modelVersion()
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

        if (!hasUserId(job, EventTypes.ANALYSIS_FAILED)) {
            return;
        }

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

    /**
     * 문자열 배열을 꺼낸다. 없거나 배열이 아니면 빈 리스트다.
     * 원소 중 null·빈 문자열은 버린다 — tag_names는 TEXT[]라 null 원소가 그대로 들어가면
     * 조회 쪽에서 매번 걸러야 한다.
     */
    private List<String> strList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .toList();
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

    /** 중첩 객체를 JSONB 컬럼용 JSON 문자열로 만든다. 비어 있으면 null(= AI가 안 보냄). */
    private String jsonObject(Map<String, Object> map, String key) {
        if (!(map.get(key) instanceof Map<?, ?> value) || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("{} 직렬화 실패 — 해당 컬럼만 비우고 저장 진행: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 문자열 배열을 JSONB 컬럼에 넣을 JSON 문자열로 만든다.
     *
     * analysis_result.key_information은 jsonb라 AnalysisResultRepository가 PGobject로
     * 바인딩한다(setJsonbOrNull). 즉 여기서 넘겨야 하는 건 이미 직렬화된 JSON 텍스트다.
     * 값이 없으면 빈 배열("[]") 대신 null을 준다 — "AI가 안 보냄"과 "보냈는데 비어 있음"을
     * 컬럼에서 구분할 수 있어야 나중에 재분석 대상을 고를 수 있다.
     *
     * 직렬화 실패는 분석 결과 전체를 버릴 이유가 못 된다. 경고만 남기고 이 칸만 비운다.
     */
    private String jsonArray(Map<String, Object> map, String key) {
        List<String> values = strList(map, key);
        if (values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.warn("{} 직렬화 실패 — 해당 컬럼만 비우고 저장 진행: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 구조화 데이터를 꺼낸다. AI가 이미지 유형에 따라 키를 달리 쓴다
     * (일정이면 scheduleData, 상품이면 structuredData). 둘 다 { type, fields } 형태다.
     * 없으면 빈 Map을 돌려줘 호출부에서 null 검사를 하지 않아도 되게 한다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> structuredData(Map<String, Object> result) {
        for (String key : List.of("structuredData", "scheduleData")) {
            if (result.get(key) instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        }
        return Map.of();
    }
}


