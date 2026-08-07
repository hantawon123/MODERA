package com.ssafy.modera.api.domain.document.service;

import com.ssafy.modera.api.domain.document.dto.response.DocumentDeleteResponse;
import com.ssafy.modera.api.domain.document.exception.DocumentErrorCode;
import com.ssafy.modera.api.domain.document.repository.DocumentCommandRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentQueryRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentViewRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 문서 삭제(8-5). 문서와 그 관계·조회 모델만 soft delete하고 원본 이미지는 건드리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDeleteService {

    private final DocumentQueryRepository documentQueryRepository;
    private final DocumentCommandRepository documentCommandRepository;
    private final DocumentViewRepository documentViewRepository;

    @Transactional
    public DocumentDeleteResponse delete(Integer userId, Integer documentId) {
        if (!documentQueryRepository.existsDocument(userId, documentId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 관계를 끄기 전에 목록을 확보한다 — 끈 뒤에는 어떤 이미지가 이 문서에 있었는지
        // 조회 모델에서 찾을 수 없다.
        List<Integer> imageIds = documentQueryRepository.findDocumentImageIds(userId, documentId);
        OffsetDateTime now = OffsetDateTime.now();

        documentCommandRepository.softDeleteDocument(userId, documentId, now);
        // 관계를 끈 뒤에 호출해야 한다 — 이 문서의 관계가 아직 살아 있으면 "다른 문서에
        // 포함되어 있다"고 판정해 표시를 끄지 못한다.
        documentViewRepository.unmarkDocumentedIfOrphan(userId, imageIds);
        log.info("문서 삭제: userId={} documentId={} images={}", userId, documentId, imageIds.size());
        return new DocumentDeleteResponse(documentId, true);
    }
}
