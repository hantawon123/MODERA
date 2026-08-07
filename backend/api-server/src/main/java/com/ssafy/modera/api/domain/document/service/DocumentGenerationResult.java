package com.ssafy.modera.api.domain.document.service;

import java.util.List;

/**
 * 저장 직전의 문서 생성 결과.
 *
 * <p>AI 응답 DTO도 이벤트 payload도 아닌 별도 타입을 두는 이유는 저장 로직이 두 경로에서
 * 공유되기 때문이다 — 동기 호출(DocumentCommandService)과 이벤트 수신
 * (DocumentResultEventHandler)이 같은 저장 절차를 쓴다.
 *
 * @param imageIds AI가 실제로 문서에 쓴 이미지. 내용이 없어 건너뛴 이미지는 빠지므로
 *                 요청 목록보다 적을 수 있고, 관계는 이 목록으로 만든다.
 */
public record DocumentGenerationResult(
        String title,
        String summary,
        String markdown,
        List<Integer> imageIds
) {
}
