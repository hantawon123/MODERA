package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterItemRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageDeleteRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageFavoriteRequest;
import com.ssafy.modera.api.domain.image.dto.response.ImageDeleteResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageFavoriteResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageRegisterResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageUploadUrlResponse;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.entity.ImageRegistrationRequest;
import com.ssafy.modera.api.domain.image.entity.Ocr;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ImageCommandRepository;
import com.ssafy.modera.api.domain.image.repository.ImageDeleteStatus;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.ImageRegistrationRequestRepository;
import com.ssafy.modera.api.domain.image.repository.OcrRepository;
import com.ssafy.modera.api.domain.image.repository.UserImageViewRow;
import com.ssafy.modera.api.domain.image.repository.UserImageViewDetail;
import com.ssafy.modera.api.domain.schedule.service.ScheduleCreationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.domain.category.repository.CategoryCommandRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeResource;
import com.ssafy.modera.api.global.config.StorageProperties;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCommandService {

    private static final Duration PUT_URL_TTL = Duration.ofMinutes(10);
    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "heic", "heif");

    private final ImageAssetRepository imageAssetRepository;
    private final ImageCommandRepository imageCommandRepository;
    private final OcrRepository ocrRepository;
    private final ImageRegistrationRequestRepository registrationRequestRepository;
    private final UserImageRepository userImageRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final CategoryCommandRepository categoryCommandRepository;
    private final StorageProperties storageProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final PlatformTransactionManager transactionManager;
    private final UserDataChangeOutboxService userDataChangeOutboxService;
    private final ScheduleCreationService scheduleCreationService;
    private final ObjectMapper objectMapper;

    public ImageRegisterResponse register(Integer userId, ImageRegisterRequest request) {
        List<ImageRegisterResponse.Registered> registered = new ArrayList<>();
        List<ImageRegisterResponse.Duplicated> duplicated = new ArrayList<>();
        List<ImageRegisterResponse.Failed> failed = new ArrayList<>();

        for (ImageRegisterItemRequest image : request.images()) {
            if (!isSupportedFormat(image.fileName())) {
                failed.add(new ImageRegisterResponse.Failed(
                        image.clientRequestId(), image.fileName(), "UNSUPPORTED_FORMAT"));
                continue;
            }

            try {
                RegistrationResult result = registerInIndependentTransaction(userId, image);
                if (result.duplicated()) {
                    duplicated.add(new ImageRegisterResponse.Duplicated(
                            image.clientRequestId(), image.fileName(), result.imageId()));
                } else {
                    registered.add(new ImageRegisterResponse.Registered(
                            image.clientRequestId(), result.imageId(), image.fileName(),
                            result.presignedUrl(), PUT_URL_TTL.toSeconds()));
                }
            } catch (BusinessException exception) {
                failed.add(new ImageRegisterResponse.Failed(
                        image.clientRequestId(), image.fileName(), exception.getErrorCode().getCode()));
            } catch (RuntimeException exception) {
                log.error("이미지 배치 등록 실패: userId={}, clientRequestId={}, fileName={}",
                        userId, image.clientRequestId(), image.fileName(), exception);
                failed.add(new ImageRegisterResponse.Failed(
                        image.clientRequestId(), image.fileName(), "INTERNAL_ERROR"));
            }
        }

        return new ImageRegisterResponse(
                List.copyOf(registered), List.copyOf(duplicated), List.copyOf(failed));
    }

    public ImageUploadUrlResponse reissueUploadUrl(Integer userId, Integer imageId) {
        userImageRepository.findByUserIdAndImageIdAndDelYn(userId, imageId, "N")
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        ImageAsset imageAsset = imageAssetRepository.findByImageIdAndDelYn(imageId, "N")
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        if ("UPLOADED".equals(imageAsset.getUploadStatus())) {
            throw new BusinessException(ImageErrorCode.UPLOAD_ALREADY_COMPLETED);
        }

        UserImageViewDetail detail = imageQueryRepository.findDetail(userId, imageId).orElse(null);
        if (detail != null
                && detail.analysisStatus() != null
                && !"NONE".equals(detail.analysisStatus())
                && !"QUEUED".equals(detail.analysisStatus())) {
            throw new BusinessException(ImageErrorCode.ANALYSIS_IN_PROGRESS);
        }

        return new ImageUploadUrlResponse(
                imageId,
                createPresignedUrl(imageAsset),
                PUT_URL_TTL.toSeconds()
        );
    }

    public ImageDeleteResponse deleteImages(Integer userId, ImageDeleteRequest request) {
        List<Integer> deletedImageIds = new ArrayList<>();
        List<Integer> alreadyDeletedImageIds = new ArrayList<>();
        List<ImageDeleteResponse.Failed> failed = new ArrayList<>();

        for (Integer imageId : new LinkedHashSet<>(request.imageIds())) {
            try {
                ImageDeleteStatus status = deleteInIndependentTransaction(userId, imageId);
                switch (status) {
                    case DELETED -> deletedImageIds.add(imageId);
                    case ALREADY_DELETED -> alreadyDeletedImageIds.add(imageId);
                    case NOT_FOUND -> failed.add(new ImageDeleteResponse.Failed(
                            imageId,
                            ImageErrorCode.IMAGE_NOT_FOUND.getCode()
                    ));
                }
            } catch (RuntimeException exception) {
                log.error("이미지 삭제 실패: userId={}, imageId={}", userId, imageId, exception);
                failed.add(new ImageDeleteResponse.Failed(imageId, "INTERNAL_ERROR"));
            }
        }

        if (!deletedImageIds.isEmpty()) {
            String deletedIdsPayload = deletedImageIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));
            userDataChangeOutboxService.record(
                    userId,
                    UserDataChangeResource.IMAGE_DELETE_BATCH,
                    deletedIdsPayload
            );
        }

        return new ImageDeleteResponse(
                List.copyOf(deletedImageIds),
                List.copyOf(alreadyDeletedImageIds),
                List.copyOf(failed),
                deletedImageIds.size(),
                failed.size()
        );
    }

    @Transactional
    public ImageFavoriteResponse updateFavorite(
            Integer userId,
            Integer imageId,
            ImageFavoriteRequest request
    ) {
        int favoriteCount = java.util.Optional.ofNullable(
                        imageCommandRepository.updateFavorite(
                                userId,
                                imageId,
                                request.favorite()
                        )
                )
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        userDataChangeOutboxService.record(
                userId, UserDataChangeResource.IMAGE_FAVORITE, String.valueOf(imageId));

        return new ImageFavoriteResponse(
                imageId,
                request.favorite(),
                favoriteCount
        );
    }

    private RegistrationResult registerInIndependentTransaction(
            Integer userId, ImageRegisterItemRequest request) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> registerOne(userId, request));
    }

    private ImageDeleteStatus deleteInIndependentTransaction(Integer userId, Integer imageId) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(
                status -> imageCommandRepository.deleteImage(userId, imageId));
    }

    private RegistrationResult registerOne(Integer userId, ImageRegisterItemRequest request) {
        ImageRegistrationRequest history = registrationRequestRepository
                .findByUserIdAndClientRequestIdAndDelYn(userId, request.clientRequestId(), "N")
                .orElse(null);

        if (history != null && history.getImageId() != null) {
            ImageAsset previousAsset = imageAssetRepository.findByImageIdAndDelYn(history.getImageId(), "N")
                    .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));
            ensureUserImage(userId, previousAsset);
            RegistrationResult replayed =
                    resultAccordingToHistory(userId, history, previousAsset);
            history.complete(previousAsset.getImageId(), replayed.duplicated(), OffsetDateTime.now());
            return replayed;
        }

        if (history == null) {
            history = registrationRequestRepository.save(new ImageRegistrationRequest(
                    userId, request.clientRequestId(), OffsetDateTime.now()));
        }

        String normalizedHash = request.contentHash().toLowerCase(Locale.ROOT);
        ImageAsset imageAsset = imageAssetRepository.findByContentHashAndDelYn(normalizedHash, "N").orElse(null);
        boolean duplicated;
        boolean reusedExistingAnalysis = false;

        if (imageAsset == null) {
            imageAsset = createImageAsset(userId, request, normalizedHash);
            ocrRepository.save(Ocr.builder()
                    .imageId(imageAsset.getImageId())
                    .content(request.ocr().rawText())
                    .build());
            createUserImageAndInitialView(userId, imageAsset);
            duplicated = false;
        } else {
            if (!imageAsset.getFileSize().equals(request.fileSize())) {
                throw new BusinessException(ImageErrorCode.DUPLICATE_IMAGE);
            }
            boolean alreadyOwned = userImageRepository
                    .findByUserIdAndImageIdAndDelYn(
                            userId, imageAsset.getImageId(), "N")
                    .isPresent();
            reusedExistingAnalysis = ensureUserImage(userId, imageAsset);
            // Duplicated means the user currently owns the image. A soft-deleted
            // relationship is restored through the ordinary registered/PUT flow.
            duplicated = alreadyOwned;
        }

        history.complete(imageAsset.getImageId(), duplicated, OffsetDateTime.now());
        // 신규 이미지나 재분석이 필요한 이미지는 ANALYSIS_COMPLETED/FAILED에서
        // 최종 결과 알림을 보낸다. 기존 분석 결과 연결만 끝난 경우에만 여기서 알린다.
        if (!duplicated && reusedExistingAnalysis) {
            userDataChangeOutboxService.record(
                    userId, UserDataChangeResource.IMAGE,
                    String.valueOf(imageAsset.getImageId()));
        }
        return duplicated ? duplicated(imageAsset.getImageId()) : registered(imageAsset);
    }

    private ImageAsset createImageAsset(
            Integer userId, ImageRegisterItemRequest request, String normalizedHash) {
        Integer imageId = imageAssetRepository.nextImageId();
        ImageAsset imageAsset = ImageAsset.builder()
                .imageId(imageId)
                .fileName(request.fileName())
                .contentHash(normalizedHash)
                .fileSize(request.fileSize())
                .s3Key("%d/%s-%s".formatted(userId, imageId, request.fileName()))
                .uploadStatus("PENDING")
                .build();
        return imageAssetRepository.save(imageAsset);
    }

    private boolean ensureUserImage(Integer userId, ImageAsset imageAsset) {
        UserImage userImage = userImageRepository
                .findByUserIdAndImageIdAndDelYn(userId, imageAsset.getImageId(), "N")
                .orElse(null);
        if (userImage == null) {
            userImage = UserImage.builder()
                    .userId(userId)
                    .imageId(imageAsset.getImageId())
                    .build();
            userImageRepository.saveAndFlush(userImage);
        }

        categoryCommandRepository.initializeFromDefault(userId, imageAsset.getImageId());
        boolean reusedExistingAnalysis =
                imageQueryRepository.copyExistingView(userId, imageAsset.getImageId());
        if (!reusedExistingAnalysis) {
            upsertInitialView(userId, imageAsset);
            return false;
        }
        createScheduleFromCopiedView(userId, imageAsset.getImageId());
        return true;
    }

    /**
     * 중복 업로드로 분석 결과를 물려받은 사용자에게도 일정 후보를 만들어 준다.
     *
     * <p>일정은 read model에만 있는 값이 아니라 schedule·user_schedule·image_schedule과
     * 조회 뷰에 행이 있어야 목록에 뜬다. 그 행을 만드는 건 분석 완료 이벤트를 받는
     * AnalysisResultEventHandler뿐인데, 중복 업로드는 분석을 다시 돌리지 않으므로 그
     * 경로를 타지 않는다. 그래서 복사된 뷰의 structured_data로 여기서 직접 만든다 —
     * 이걸 빼면 "상세에는 일정 정보가 보이는데 일정 목록에는 없는" 상태가 된다.
     *
     * <p>카테고리는 initializeFromDefault가, 태그는 read model 복사가 이미 처리한다.
     * 별도 테이블에 행이 필요한 파생 데이터는 일정뿐이다.
     */
    private void createScheduleFromCopiedView(Integer userId, Integer imageId) {
        imageQueryRepository.findDetail(userId, imageId).ifPresent(detail -> {
            String structuredDataJson = detail.structuredDataJson();
            if (structuredDataJson == null || structuredDataJson.isBlank()) {
                return;
            }
            try {
                // 복사된 값은 {"type": "...", "fields": {...}} 형태다(AnalysisResultEventHandler가
                // 그렇게 조립해 넣는다). createFromAnalysis는 둘을 나눠 받으므로 여기서 푼다.
                JsonNode structuredData = objectMapper.readTree(structuredDataJson);
                JsonNode type = structuredData.get("type");
                JsonNode fields = structuredData.get("fields");
                if (type == null || type.isNull()) {
                    return;
                }
                scheduleCreationService.createFromAnalysis(
                        userId,
                        imageId,
                        detail.title(),
                        type.asText(),
                        fields == null || fields.isNull() ? null : fields.toString()
                );
            } catch (Exception exception) {
                // 일정 생성 실패로 이미지 등록 자체를 되돌리지 않는다. 등록은 성공시키고
                // 일정만 비는 편이 사용자에게 낫다(AnalysisResultEventHandler와 같은 판단).
                log.warn("중복 업로드 이미지의 일정 생성 실패: userId={} imageId={}",
                        userId, imageId, exception);
            }
        });
    }

    private void createUserImageAndInitialView(Integer userId, ImageAsset imageAsset) {
        userImageRepository.save(UserImage.builder()
                .userId(userId)
                .imageId(imageAsset.getImageId())
                .build());
        upsertInitialView(userId, imageAsset);
    }

    private void upsertInitialView(Integer userId, ImageAsset imageAsset) {
        imageQueryRepository.upsert(new UserImageViewRow(
                userId, imageAsset.getImageId(), imageAsset.getFileName(), imageAsset.getS3Key(),
                null, imageAsset.getFileName(), null, null, null, List.of(), List.of(), null, null,
                imageAsset.getUploadStatus(), "NONE", false, imageAsset.getUploadedAt()));
    }

    private RegistrationResult resultAccordingToObjectExistence(ImageAsset imageAsset) {
        return objectExists(imageAsset) ? duplicated(imageAsset.getImageId()) : registered(imageAsset);
    }

    private RegistrationResult resultAccordingToHistory(
            Integer userId,
            ImageRegistrationRequest history,
            ImageAsset imageAsset) {
        if ("REGISTERED".equals(history.getStatus())) {
            return registered(imageAsset);
        }
        if ("DUPLICATED".equals(history.getStatus())) {
            if (!imageQueryRepository.isAnalysisActiveOrCompleted(
                    userId, imageAsset.getImageId())) {
                return registered(imageAsset);
            }
            return duplicated(imageAsset.getImageId());
        }
        return resultAccordingToObjectExistence(imageAsset);
    }

    private boolean objectExists(ImageAsset imageAsset) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(storageProperties.getBucket().getPictures())
                    .key(imageAsset.getS3Key())
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

    private RegistrationResult registered(ImageAsset imageAsset) {
        return new RegistrationResult(imageAsset.getImageId(), false, createPresignedUrl(imageAsset));
    }

    private RegistrationResult duplicated(Integer imageId) {
        return new RegistrationResult(imageId, true, null);
    }

    private String createPresignedUrl(ImageAsset imageAsset) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket().getPictures())
                .key(imageAsset.getS3Key())
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PUT_URL_TTL)
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return presigned.url().toString();
    }

    private boolean isSupportedFormat(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator >= 0
                && separator < fileName.length() - 1
                && SUPPORTED_EXTENSIONS.contains(fileName.substring(separator + 1).toLowerCase(Locale.ROOT));
    }

    private record RegistrationResult(Integer imageId, boolean duplicated, String presignedUrl) {
    }
}
