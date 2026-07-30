package com.ssafy.modera.contract.payload;

import java.util.List;

/**
 * {@link com.ssafy.modera.contract.EventTypes#DOCUMENT_REQUESTED} 이벤트 payload.
 * api-server가 발행하고 analysis-worker가 소비한다(스트림: image-analysis).
 *
 * <p>worker는 문서를 저장하지 않는다 — 여기 실린 재료에 자기 DB의 OCR
 * (analysis_result.ocr_refined_text)을 보태 AI에 넘기고, 결과 마크다운을
 * DOCUMENT_COMPLETED로 되돌려 보내는 중개만 한다. OCR이 worker DB에만 있어서
 * worker가 경유지가 되는 것뿐, 문서의 원본 저장(document_schema)은 api 몫이다.
 *
 * @param documentRequestId api-server가 만든 멱등키. <b>필수</b> — 없으면 worker는
 *                          처리를 거부한다(폴백 생성 금지, DocumentGenerationService 참고).
 * @param instruction       사용자가 입력한 지시문. null/blank면 worker가 기본 문구로 대체한다.
 * @param images            문서 재료. <b>순서를 유지해야 한다</b> — 첫 번째가 기본(중심)
 *                          자료라는 전제로 AI에 그대로 전달된다.
 */
public record DocumentRequestedPayload(
        String documentRequestId,
        int userId,
        String instruction,
        List<SourceImage> images
) {
    /** createdAt은 ISO-8601 문자열(다른 payload의 시각 필드와 동일한 관례). */
    public record SourceImage(
            int imageId,
            String title,
            String categoryName,
            List<String> tagNames,
            List<String> keyInformation,
            String summary,
            String createdAt
    ) {
    }
}
