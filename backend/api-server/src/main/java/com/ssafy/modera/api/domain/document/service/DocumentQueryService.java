package com.ssafy.modera.api.domain.document.service;

import com.ssafy.modera.api.domain.document.dto.response.DocumentDetailResponse;
import com.ssafy.modera.api.domain.document.dto.response.DocumentImageResponse;
import com.ssafy.modera.api.domain.document.dto.response.DocumentSummaryResponse;
import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import com.ssafy.modera.api.domain.document.exception.DocumentErrorCode;
import com.ssafy.modera.api.domain.document.repository.DocumentDetailRow;
import com.ssafy.modera.api.domain.document.repository.DocumentGenerationRequestRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentImageIdRow;
import com.ssafy.modera.api.domain.document.repository.DocumentQueryRepository;
import com.ssafy.modera.api.domain.image.service.ThumbnailUrlFactory;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import com.ssafy.modera.api.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 문서 조회(8-1 목록, 8-3 상세, 8-4 구성 이미지).
 *
 * <p>모두 query_schema의 조회 모델만 읽는다. 소유권은 조회 조건(user_id)이 곧 검증이라
 * 별도 확인이 없고, 행이 없으면 존재하지 않는 것과 남의 것을 구분하지 않고 404다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentQueryService {

    private static final String DEFAULT_SORT = "UPDATED_DESC";
    private static final Set<String> SUPPORTED_SORTS =
            Set.of("UPDATED_DESC", "UPDATED_ASC", "NAME_ASC");

    private final DocumentQueryRepository documentQueryRepository;
    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final ThumbnailUrlFactory thumbnailUrlFactory;

    public PageResponse<DocumentSummaryResponse> getDocuments(
            Integer userId, int page, int size, String sort) {
        validatePaging(page, size);
        String normalizedSort = normalizeSort(sort, DEFAULT_SORT, SUPPORTED_SORTS);

        long totalElements = documentQueryRepository.countDocuments(userId);
        List<DocumentSummaryResponse> list = documentQueryRepository
                .findDocuments(userId, normalizedSort, page, size)
                .stream()
                .map(row -> new DocumentSummaryResponse(
                        row.documentId(),
                        row.name(),
                        row.summary(),
                        row.imageCount(),
                        row.delImageCount(),
                        row.updatedAt()
                ))
                .toList();

        return toPage(list, page, size, totalElements);
    }

    public DocumentDetailResponse getDocument(Integer userId, Integer documentId) {
        DocumentDetailRow row = documentQueryRepository.findDocument(userId, documentId)
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND));

        return new DocumentDetailResponse(
                row.documentId(),
                row.name(),
                row.summary(),
                row.content(),
                row.imageCount(),
                row.delImageCount(),
                documentQueryRepository.findDocumentImageIds(userId, documentId),
                isRegenerating(userId, documentId),
                row.updatedAt()
        );
    }

    /** 본인의 삭제되지 않은 모든 문서를 페이지네이션 없이 상세 형식으로 반환한다. */
    public List<DocumentDetailResponse> getAllDocumentDetails(Integer userId) {
        List<DocumentDetailRow> documents = documentQueryRepository.findAllDocumentDetails(userId);
        if (documents.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<Integer>> imageIdsByDocument = documentQueryRepository
                .findAllDocumentImageIds(userId)
                .stream()
                .collect(Collectors.groupingBy(
                        DocumentImageIdRow::documentId,
                        LinkedHashMap::new,
                        Collectors.mapping(DocumentImageIdRow::imageId, Collectors.toList())
                ));
        Set<Integer> regeneratingDocumentIds = new HashSet<>(
                documentGenerationRequestRepository.findSourceDocumentIdsByUserIdAndStatus(
                        userId, DocumentGenerationRequest.STATUS_QUEUED));

        return documents.stream()
                .map(row -> new DocumentDetailResponse(
                        row.documentId(),
                        row.name(),
                        row.summary(),
                        row.content(),
                        row.imageCount(),
                        row.delImageCount(),
                        List.copyOf(imageIdsByDocument.getOrDefault(row.documentId(), List.of())),
                        regeneratingDocumentIds.contains(row.documentId()),
                        row.updatedAt()
                ))
                .toList();
    }

    /**
     * 문서 구성 이미지(8-4). 한 문서에 넣을 수 있는 이미지가 최대 30장
     * (DocumentCommandService.MAX_IMAGES)이라 페이지를 나누지 않고 전부 준다.
     */
    public List<DocumentImageResponse> getDocumentImages(Integer userId, Integer documentId) {
        if (!documentQueryRepository.existsDocument(userId, documentId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        return documentQueryRepository.findDocumentImages(userId, documentId)
                .stream()
                .map(row -> new DocumentImageResponse(
                        row.imageId(),
                        row.title(),
                        row.summary(),
                        thumbnailUrlFactory.createViewUrl(row.thumbnailKey()),
                        row.tags(),
                        row.addedAt()
                ))
                .toList();
    }

    private boolean isRegenerating(Integer userId, Integer documentId) {
        return documentGenerationRequestRepository
                .existsByUserIdAndSourceDocumentIdAndStatusAndDelYn(
                        userId, documentId, DocumentGenerationRequest.STATUS_QUEUED, "N");
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }
    }

    private String normalizeSort(String sort, String defaultSort, Set<String> supported) {
        String normalized = sort == null || sort.isBlank()
                ? defaultSort
                : sort.trim().toUpperCase(Locale.ROOT);
        if (!supported.contains(normalized)) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }
        return normalized;
    }

    private <T> PageResponse<T> toPage(List<T> list, int page, int size, long totalElements) {
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(
                list,
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages,
                page > 0
        );
    }
}
