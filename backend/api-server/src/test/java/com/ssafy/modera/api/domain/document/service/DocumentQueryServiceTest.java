package com.ssafy.modera.api.domain.document.service;

import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import com.ssafy.modera.api.domain.document.repository.DocumentDetailRow;
import com.ssafy.modera.api.domain.document.repository.DocumentGenerationRequestRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentImageIdRow;
import com.ssafy.modera.api.domain.document.repository.DocumentQueryRepository;
import com.ssafy.modera.api.domain.image.service.ThumbnailUrlFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentQueryServiceTest {

    @Mock DocumentQueryRepository documentQueryRepository;
    @Mock DocumentGenerationRequestRepository documentGenerationRequestRepository;
    @Mock ThumbnailUrlFactory thumbnailUrlFactory;
    @InjectMocks DocumentQueryService documentQueryService;

    @Test
    void returnsEveryDocumentInDetailShapeWithoutPagination() {
        OffsetDateTime latest = OffsetDateTime.parse("2026-08-06T12:00:00+09:00");
        OffsetDateTime older = OffsetDateTime.parse("2026-08-05T12:00:00+09:00");
        when(documentQueryRepository.findAllDocumentDetails(7)).thenReturn(List.of(
                new DocumentDetailRow(20, "latest", "summary-20", "# content-20", 2, 0, latest),
                new DocumentDetailRow(10, "older", "summary-10", "# content-10", 1, 1, older)
        ));
        when(documentQueryRepository.findAllDocumentImageIds(7)).thenReturn(List.of(
                new DocumentImageIdRow(10, 101),
                new DocumentImageIdRow(20, 201),
                new DocumentImageIdRow(20, 202)
        ));
        when(documentGenerationRequestRepository.findSourceDocumentIdsByUserIdAndStatus(
                7, DocumentGenerationRequest.STATUS_QUEUED)).thenReturn(List.of(20));

        var result = documentQueryService.getAllDocumentDetails(7);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).documentId()).isEqualTo(20);
        assertThat(result.get(0).content()).isEqualTo("# content-20");
        assertThat(result.get(0).imageIds()).containsExactly(201, 202);
        assertThat(result.get(0).regenerating()).isTrue();
        assertThat(result.get(1).documentId()).isEqualTo(10);
        assertThat(result.get(1).imageIds()).containsExactly(101);
        assertThat(result.get(1).regenerating()).isFalse();
    }

    @Test
    void returnsEmptyListWithoutAdditionalQueriesWhenUserHasNoDocuments() {
        when(documentQueryRepository.findAllDocumentDetails(7)).thenReturn(List.of());

        assertThat(documentQueryService.getAllDocumentDetails(7)).isEmpty();

        verifyNoInteractions(documentGenerationRequestRepository);
    }
}
