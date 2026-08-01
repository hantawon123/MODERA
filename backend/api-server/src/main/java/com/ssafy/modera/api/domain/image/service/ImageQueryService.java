package com.ssafy.modera.api.domain.image.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.domain.image.dto.response.ImageDetailResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageSummaryResponse;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageListPage;
import com.ssafy.modera.api.domain.image.repository.ImageListRow;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.UserImageViewDetail;
import com.ssafy.modera.api.global.config.StorageProperties;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import com.ssafy.modera.api.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageQueryService {

    private static final Duration THUMBNAIL_URL_TTL = Duration.ofHours(1);
    private static final Set<String> SUPPORTED_SORTS =
            Set.of("TITLE_ASC", "UPLOADED_DESC", "UPLOADED_ASC");

    private final ImageQueryRepository imageQueryRepository;
    private final StorageProperties storageProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ObjectMapper objectMapper;

    public ImageDetailResponse getImage(Integer userId, Integer imageId) {
        UserImageViewDetail detail = imageQueryRepository
                .findDetail(userId, imageId)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        if (!"COMPLETED".equals(detail.analysisStatus())
                && !"EMPTY".equals(detail.analysisStatus())) {
            throw new BusinessException(ImageErrorCode.IMAGE_ANALYSIS_NOT_COMPLETED);
        }

        return new ImageDetailResponse(
                imageId,
                createImageUrl(detail.s3Key()),
                createThumbnailUrl(detail.thumbnailKey()),
                detail.title(),
                Boolean.TRUE.equals(detail.favorite()),
                detail.summary(),
                detail.categoryName(),
                detail.tagNames(),
                detail.keyInformation(),
                parseStructuredData(detail.structuredDataJson()),
                detail.ocrRawText(),
                detail.uploadedAt(),
                detail.documented(),
                detail.calendared()
        );
    }

    public PageResponse<ImageSummaryResponse> getImages(
            Integer userId,
            Boolean favorite,
            int page,
            int size,
            String sort,
            String keyword,
            Integer categoryId
    ) {
        if (page < 0 || size < 1 || size > 100 || categoryId != null && categoryId < 1) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }

        String normalizedSort = sort == null || sort.isBlank()
                ? "UPLOADED_DESC"
                : sort.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SORTS.contains(normalizedSort)) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }
        String normalizedKeyword = keyword == null || keyword.trim().isEmpty()
                ? null
                : keyword.trim();

        ImageListPage result = imageQueryRepository.findImages(
                userId, favorite, categoryId, normalizedKeyword, normalizedSort, page, size);
        List<ImageSummaryResponse> list = result.content().stream()
                .map(this::toSummary)
                .toList();
        int totalPages = result.totalElements() == 0
                ? 0
                : (int) ((result.totalElements() + size - 1) / size);

        return new PageResponse<>(
                list,
                page,
                size,
                result.totalElements(),
                totalPages,
                page + 1 < totalPages,
                page > 0
        );
    }

    public PageResponse<ImageSummaryResponse> getImagesInOrder(
            Integer userId,
            List<Integer> orderedImageIds,
            int page,
            int size,
            long totalElements
    ) {
        List<ImageListRow> rows =
                imageQueryRepository.findVisibleImagesByIds(userId, orderedImageIds);
        Map<Integer, ImageListRow> rowsByImageId = new LinkedHashMap<>();
        for (ImageListRow row : rows) {
            rowsByImageId.put(row.imageId(), row);
        }

        List<ImageSummaryResponse> list = orderedImageIds.stream()
                .distinct()
                .map(rowsByImageId::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toSummary)
                .toList();
        int totalPages = totalElements == 0
                ? 0
                : (int) ((totalElements + size - 1) / size);

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

    private ImageSummaryResponse toSummary(ImageListRow row) {
        return new ImageSummaryResponse(
                row.imageId(),
                row.title(),
                row.summary(),
                Boolean.TRUE.equals(row.favorite()),
                createThumbnailUrl(row.thumbnailKey()),
                row.tagNames(),
                row.categoryName(),
                row.uploadedAt()
        );
    }

    private String createThumbnailUrl(String thumbnailKey) {
        if (thumbnailKey == null || thumbnailKey.isBlank() || !thumbnailExists(thumbnailKey)) {
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket().getThumbnails())
                .key(thumbnailKey)
                .build();
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(THUMBNAIL_URL_TTL)
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }

    private String createImageUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket().getPictures())
                .key(s3Key)
                .build();
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(THUMBNAIL_URL_TTL)
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }

    private JsonNode parseStructuredData(String structuredDataJson) {
        if (structuredDataJson == null || structuredDataJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(structuredDataJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("이미지 구조화 데이터를 읽을 수 없습니다.", exception);
        }
    }

    private boolean thumbnailExists(String thumbnailKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.getBucket().getThumbnails())
                    .key(thumbnailKey)
                    .build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }
}
