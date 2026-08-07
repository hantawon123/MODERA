package com.ssafy.modera.api.domain.document.service;

import com.ssafy.modera.api.domain.document.client.DocumentAiClient;
import com.ssafy.modera.api.domain.document.dto.request.DocumentCreateRequest;
import com.ssafy.modera.api.domain.document.dto.request.DocumentImagesRequest;
import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import com.ssafy.modera.api.domain.document.exception.DocumentErrorCode;
import com.ssafy.modera.api.domain.document.repository.DocumentGenerationRequestRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentQueryRepository;
import com.ssafy.modera.api.domain.image.entity.Ocr;
import com.ssafy.modera.api.domain.image.repository.DocumentSourceImage;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.OcrRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 동기 문서 생성의 AI 요청 매핑과 실패 처리 검증.
 *
 * <p>AI의 DocumentImage는 title·summary·tags·keyInformation·ocr을 "Optional이 아니고
 * 기본값만 있는" 필드로 선언해서, 명시적 null을 보내면 요청 전체가 400으로 거절된다
 * (worker에서 실제로 겪은 장애다). api가 직접 호출하게 되면서 같은 함정이 이쪽으로
 * 옮겨왔으므로 여기서도 막아 둔다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentCommandServiceTest {

    private static final Integer USER_ID = 3;

    @Mock
    private ImageQueryRepository imageQueryRepository;
    @Mock
    private DocumentQueryRepository documentQueryRepository;
    @Mock
    private DocumentGenerationRequestRepository documentGenerationRequestRepository;
    @Mock
    private OcrRepository ocrRepository;
    @Mock
    private DocumentAiClient documentAiClient;
    @Mock
    private DocumentPersistService documentPersistService;
    @Mock
    private DocumentQueryService documentQueryService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private DocumentCommandService documentCommandService;

    @BeforeEach
    void setUp() {
        given(transactionTemplate.execute(any()))
                .willAnswer(invocation -> invocation.<TransactionCallback<?>>getArgument(0)
                        .doInTransaction(null));
        // 생성은 save, 재분석·추가·제외는 saveAndFlush를 쓴다(유니크 인덱스 위반을 그 자리에서
        // 잡아 409로 번역해야 해서). 둘 다 저장한 엔티티를 그대로 돌려주게 둔다.
        given(documentGenerationRequestRepository.save(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(documentGenerationRequestRepository.saveAndFlush(any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(documentPersistService.persist(any(), any())).willReturn(101);
    }

    @Test
    @DisplayName("OCR이 없어도 ocr을 null로 보내지 않는다")
    void sendsEmptyOcrInsteadOfNull() {
        givenSources(source(7, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any())).willReturn(aiResponse());

        documentCommandService.create(USER_ID, createRequest(7));

        DocumentAiClient.SourceImage sent = captureFirstImage();
        assertThat(sent.ocr()).isNotNull();
        assertThat(sent.ocr().rawText()).isEmpty();
    }

    @Test
    @DisplayName("앱이 보낸 OCR 원문을 rawText로 싣는다")
    void sendsStoredOcr() {
        givenSources(source(7, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList()))
                .willReturn(List.of(Ocr.builder().imageId(7).content("소니 WF-1000XM5 359,000원").build()));
        given(documentAiClient.generate(any())).willReturn(aiResponse());

        documentCommandService.create(USER_ID, createRequest(7));

        assertThat(captureFirstImage().ocr().rawText()).isEqualTo("소니 WF-1000XM5 359,000원");
    }

    @Test
    @DisplayName("조회 모델의 값이 비어도 null로 보내지 않는다")
    void neverSendsNullDefaultedFields() {
        givenSources(new DocumentSourceImage(7, null, null, null, null, null, null, "COMPLETED"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any())).willReturn(aiResponse());

        documentCommandService.create(USER_ID, createRequest(7));

        DocumentAiClient.SourceImage sent = captureFirstImage();
        assertThat(sent.title()).isEmpty();
        assertThat(sent.summary()).isEmpty();
        assertThat(sent.tags()).isEmpty();
        assertThat(sent.keyInformation()).isEmpty();
        assertThat(sent.ocr()).isNotNull();
    }

    @Test
    @DisplayName("AI가 4xx로 거절하면 요청을 실패로 닫고 DOCUMENT_AI_REJECTED로 응답한다")
    void marksFailedOnRejection() {
        givenSources(source(7, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any()))
                .willThrow(HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request", null, null, null));

        assertThatThrownBy(() -> documentCommandService.create(USER_ID, createRequest(7)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_AI_REJECTED);

        verify(documentPersistService).markFailed(any(), eq("DOCUMENT_AI_REJECTED"));
    }

    @Test
    @DisplayName("AI 타임아웃이면 재시도 가치가 있는 DOCUMENT_GENERATION_FAILED로 응답한다")
    void marksFailedOnTimeout() {
        givenSources(source(7, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any()))
                .willThrow(new ResourceAccessException("read timed out"));

        assertThatThrownBy(() -> documentCommandService.create(USER_ID, createRequest(7)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_GENERATION_FAILED);

        verify(documentPersistService).markFailed(any(), eq("DOCUMENT_AI_ERROR"));
    }

    @Test
    @DisplayName("markdown이 비어 있으면 빈 문서를 저장하지 않는다")
    void rejectsEmptyMarkdown() {
        givenSources(source(7, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any())).willReturn(new DocumentAiClient.DocumentResponse(
                "제목", "요약", "   ", List.of(7), "mock", null));

        assertThatThrownBy(() -> documentCommandService.create(USER_ID, createRequest(7)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_GENERATION_FAILED);

        verify(documentPersistService).markFailed(any(), eq("DOCUMENT_EMPTY_RESULT"));
    }

    @Test
    @DisplayName("완료된 요청을 같은 키로 다시 보내면 AI를 부르지 않고 그 문서를 돌려준다")
    void replaysCompletedRequest() {
        DocumentGenerationRequest completed =
                DocumentGenerationRequest.create(USER_ID, UUID.randomUUID(), OffsetDateTime.now());
        completed.complete(101, OffsetDateTime.now());
        givenExisting(completed);
        given(documentQueryRepository.existsDocument(USER_ID, 101)).willReturn(true);

        documentCommandService.create(USER_ID, createRequest(7));

        verify(documentAiClient, never()).generate(any());
        verify(documentQueryService).getDocument(USER_ID, 101);
    }

    @Test
    @DisplayName("완료됐지만 그 문서가 삭제됐으면 되살리지 않고 409로 끊는다")
    void rejectsReplayWhenDocumentDeleted() {
        DocumentGenerationRequest completed =
                DocumentGenerationRequest.create(USER_ID, UUID.randomUUID(), OffsetDateTime.now());
        completed.complete(101, OffsetDateTime.now());
        givenExisting(completed);
        given(documentQueryRepository.existsDocument(USER_ID, 101)).willReturn(false);

        assertThatThrownBy(() -> documentCommandService.create(USER_ID, createRequest(7)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DUPLICATE_CLIENT_REQUEST);

        verify(documentAiClient, never()).generate(any());
    }

    @Test
    @DisplayName("아직 처리 중인 요청을 같은 키로 다시 보내면 진행 중이라고 알린다")
    void rejectsWhileInProgress() {
        givenExisting(DocumentGenerationRequest.create(USER_ID, UUID.randomUUID(), OffsetDateTime.now()));

        assertThatThrownBy(() -> documentCommandService.create(USER_ID, createRequest(7)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", DocumentErrorCode.DOCUMENT_GENERATION_IN_PROGRESS);

        verify(documentAiClient, never()).generate(any());
    }

    @Test
    @DisplayName("실패한 요청은 같은 키로 다시 실행한다")
    void rerunsFailedRequest() {
        DocumentGenerationRequest failed =
                DocumentGenerationRequest.create(USER_ID, UUID.randomUUID(), OffsetDateTime.now());
        failed.fail("DOCUMENT_AI_ERROR", OffsetDateTime.now());
        givenExisting(failed);
        givenSources(source(7, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any())).willReturn(aiResponse());

        documentCommandService.create(USER_ID, createRequest(7));

        verify(documentAiClient).generate(any());
        assertThat(failed.isQueued()).isTrue();
        assertThat(failed.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("처리 도중 끊겨 QUEUED로 굳은 요청도 같은 키로 다시 실행한다")
    void rerunsStaleQueuedRequest() {
        DocumentGenerationRequest stale = DocumentGenerationRequest.create(
                USER_ID, UUID.randomUUID(), OffsetDateTime.now().minusMinutes(10));
        givenExisting(stale);
        givenSources(source(7, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any())).willReturn(aiResponse());

        documentCommandService.create(USER_ID, createRequest(7));

        verify(documentAiClient).generate(any());
    }

    @Test
    @DisplayName("이미지 추가는 현재 구성에 요청 이미지를 더한 목록으로 다시 만든다")
    void addsImagesToCurrentComposition() {
        givenDocumentImages(7, 8);
        givenSources(source(7, "제목", "요약"), source(8, "제목", "요약"), source(9, "제목", "요약"));
        given(ocrRepository.findByImageIdIn(anyList())).willReturn(List.of());
        given(documentAiClient.generate(any())).willReturn(aiResponse());

        documentCommandService.addImages(USER_ID, 101, new DocumentImagesRequest(
                UUID.randomUUID(), List.of(8, 9)));   // 8은 이미 포함 — 중복 제거된다

        assertThat(capturedImageIds()).containsExactly(7, 8, 9);
    }

    private void givenDocumentImages(Integer... imageIds) {
        given(documentQueryRepository.existsDocument(USER_ID, 101)).willReturn(true);
        given(documentQueryRepository.findDocumentImageIds(USER_ID, 101)).willReturn(List.of(imageIds));
    }

    private List<Integer> capturedImageIds() {
        ArgumentCaptor<DocumentAiClient.DocumentRequest> captor =
                ArgumentCaptor.forClass(DocumentAiClient.DocumentRequest.class);
        verify(documentAiClient).generate(captor.capture());
        return captor.getValue().images().stream().map(DocumentAiClient.SourceImage::imageId).toList();
    }

    private void givenExisting(DocumentGenerationRequest existing) {
        given(documentGenerationRequestRepository.findByUserIdAndClientRequestIdAndDelYn(
                any(), any(), any())).willReturn(java.util.Optional.of(existing));
    }

    /**
     * 기존 요청 이력 조회는 스텁하지 않는다 — Mockito 기본값이 Optional.empty()라
     * "처음 보는 요청"이 되고, 재시도 시나리오는 givenExisting()이 따로 덮어쓴다.
     */
    private void givenSources(DocumentSourceImage... sources) {
        given(imageQueryRepository.findDocumentSources(eq(USER_ID), anyList()))
                .willReturn(List.of(sources));
    }

    private DocumentAiClient.SourceImage captureFirstImage() {
        ArgumentCaptor<DocumentAiClient.DocumentRequest> captor =
                ArgumentCaptor.forClass(DocumentAiClient.DocumentRequest.class);
        verify(documentAiClient).generate(captor.capture());
        return captor.getValue().images().getFirst();
    }

    private DocumentSourceImage source(Integer imageId, String title, String summary) {
        return new DocumentSourceImage(imageId, title, summary, "쇼핑",
                List.of("이어폰"), List.of("가격: 359,000원"), OffsetDateTime.now(), "COMPLETED");
    }

    private DocumentCreateRequest createRequest(Integer... imageIds) {
        return new DocumentCreateRequest(UUID.randomUUID(), List.of(imageIds));
    }

    private DocumentAiClient.DocumentResponse aiResponse() {
        return new DocumentAiClient.DocumentResponse(
                "제목", "요약", "# 제목\n\n본문", List.of(7), "mock", null);
    }

    /** create()가 만든 요청 엔티티는 저장 스텁이 그대로 돌려주므로 ID가 없다 — 검증에 쓰지 않는다. */
    @SuppressWarnings("unused")
    private DocumentGenerationRequest unusedRequest() {
        return DocumentGenerationRequest.create(USER_ID, UUID.randomUUID(), OffsetDateTime.now());
    }
}
