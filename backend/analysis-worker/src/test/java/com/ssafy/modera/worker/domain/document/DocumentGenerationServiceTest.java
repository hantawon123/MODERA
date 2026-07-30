package com.ssafy.modera.worker.domain.document;

import com.ssafy.modera.worker.domain.document.client.DocumentAiClient;
import com.ssafy.modera.worker.domain.document.repository.DocumentOcr;
import com.ssafy.modera.worker.domain.document.repository.DocumentOcrRepository;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import com.ssafy.modera.contract.payload.DocumentRequestedPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/**
 * AI 요청 재료 매핑 검증.
 *
 * <p>AI의 DocumentImage는 title·summary·tags·keyInformation·ocr을 "Optional이 아니고
 * 기본값만 있는" 필드로 선언한다. 명시적 null을 보내면 pydantic 검증에서 걸려 400
 * (INVALID_REQUEST)로 요청 전체가 거절되고, worker는 그걸 DOCUMENT_AI_REJECTED
 * (재시도 불가)로 끊는다 — 실제로 문서 생성이 통째로 실패했던 원인이다.
 */
@ExtendWith(MockitoExtension.class)
class DocumentGenerationServiceTest {

    @Mock
    private DocumentOcrRepository documentOcrRepository;
    @Mock
    private DocumentAiClient documentAiClient;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private DocumentGenerationService documentGenerationService;

    @Test
    @DisplayName("OCR이 없어도 ocr을 null로 보내지 않는다")
    void sendsEmptyOcrInsteadOfNull() {
        given(documentOcrRepository.findLatestOcr(anyList())).willReturn(Map.of());
        given(documentAiClient.generate(any())).willReturn(response());

        documentGenerationService.handle(payload(sourceImage()));

        DocumentAiClient.SourceImage sent = captureFirstImage();
        assertThat(sent.ocr()).isNotNull();
        assertThat(sent.ocr().rawText()).isEmpty();
        assertThat(sent.ocr().refinedText()).isNull();
    }

    @Test
    @DisplayName("refined가 없으면 raw OCR을 rawText로 싣는다")
    void sendsRawOcrWhenRefinedMissing() {
        given(documentOcrRepository.findLatestOcr(anyList()))
                .willReturn(Map.of(6, new DocumentOcr("소니 WF-1000XM5 359,000원", null)));
        given(documentAiClient.generate(any())).willReturn(response());

        documentGenerationService.handle(payload(sourceImage()));

        DocumentAiClient.Ocr ocr = captureFirstImage().ocr();
        assertThat(ocr.rawText()).isEqualTo("소니 WF-1000XM5 359,000원");
        assertThat(ocr.refinedText()).isNull();
    }

    @Test
    @DisplayName("정제본이 있으면 refinedText로 싣는다")
    void prefersRefinedOcr() {
        given(documentOcrRepository.findLatestOcr(anyList()))
                .willReturn(Map.of(6, new DocumentOcr("raw", "refined")));
        given(documentAiClient.generate(any())).willReturn(response());

        documentGenerationService.handle(payload(sourceImage()));

        assertThat(captureFirstImage().ocr().refinedText()).isEqualTo("refined");
    }

    @Test
    @DisplayName("payload의 title·summary·tags·keyInformation이 비어도 null로 보내지 않는다")
    void neverSendsNullDefaultedFields() {
        given(documentOcrRepository.findLatestOcr(anyList())).willReturn(Map.of());
        given(documentAiClient.generate(any())).willReturn(response());

        documentGenerationService.handle(payload(new DocumentRequestedPayload.SourceImage(
                6, null, null, null, null, null, null)));

        DocumentAiClient.SourceImage sent = captureFirstImage();
        assertThat(sent.title()).isEmpty();
        assertThat(sent.summary()).isEmpty();
        assertThat(sent.tags()).isEmpty();
        assertThat(sent.keyInformation()).isEmpty();
        assertThat(sent.ocr()).isNotNull();
    }

    private DocumentAiClient.SourceImage captureFirstImage() {
        ArgumentCaptor<DocumentAiClient.DocumentRequest> captor =
                ArgumentCaptor.forClass(DocumentAiClient.DocumentRequest.class);
        org.mockito.Mockito.verify(documentAiClient).generate(captor.capture());
        return captor.getValue().images().getFirst();
    }

    private DocumentRequestedPayload.SourceImage sourceImage() {
        return new DocumentRequestedPayload.SourceImage(
                6, "노이즈 캔슬링 이어폰", "쇼핑", List.of("이어폰"),
                List.of("가격: 359,000원"), "이어폰 비교", "2026-07-30T12:00:00.000Z");
    }

    private DocumentRequestedPayload payload(DocumentRequestedPayload.SourceImage image) {
        return new DocumentRequestedPayload("2", 3, null, List.of(image));
    }

    private DocumentAiClient.DocumentResponse response() {
        return new DocumentAiClient.DocumentResponse(
                "제목", "요약", "# 제목", List.of(), List.of(6), "mock", null);
    }
}
