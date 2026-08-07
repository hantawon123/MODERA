package com.ssafy.modera.worker.domain.document.repository;

/**
 * 문서 재료로 쓸 이미지 한 장의 OCR.
 *
 * <p>raw와 refined를 합치지 않고 따로 들고 있는 이유는 AI가 둘을 구분해서 쓰기 때문이다
 * (refined가 있으면 그쪽을 우선). 여기서 미리 하나로 합치면 어느 쪽 텍스트인지가 사라져
 * AI 요청에 정직하게 실을 수 없다.
 *
 * <p>현재 refinedText는 사실상 항상 null이다 — AI가 2026-07-29 합의로 콜백에서
 * ocrRefinedText를 빼면서 analysis_result.ocr_refined_text가 채워지지 않는다. 그래도
 * 컬럼과 필드를 남겨 두는 건 AI가 다시 정제본을 돌려주기 시작하면 그대로 우선 적용되게
 * 하기 위해서다.
 */
public record DocumentOcr(String rawText, String refinedText) {

    public boolean isEmpty() {
        return isBlank(rawText) && isBlank(refinedText);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
