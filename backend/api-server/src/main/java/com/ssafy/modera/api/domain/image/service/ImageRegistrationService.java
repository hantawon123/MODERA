package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.dto.ImageRegisterItemRequest;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterResponse;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRepository;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRow;
import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.global.config.StorageProperties;
import com.ssafy.modera.api.global.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageRegistrationService {

    private static final Duration PUT_URL_TTL = Duration.ofMinutes(10);

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "heic", "heif");

    private final ImageAssetRepository imageAssetRepository;
    private final UserImageRepository userImageRepository;
    private final UserRepository userRepository;
    private final UserImageViewRepository userImageViewRepository;
    private final StorageProperties storageProperties;
    private final S3Presigner s3Presigner;
    private final Validator validator;
    private final PlatformTransactionManager transactionManager;

    /**
     * 배치는 부분 성공을 허용한다.
     * 이미지마다 독립 트랜잭션으로 처리하여 한 이미지의 실패가
     * 다른 이미지의 등록 결과에 영향을 주지 않도록 한다.
     *
     * OCR 값은 필수 검증만 수행하며 아직 영속화하지 않는다.
     */
    public ImageRegisterResponse register(
            Integer userId,
            ImageRegisterRequest request
    ) {
        List<ImageRegisterResponse.Registered> registered =
                new ArrayList<>();

        List<ImageRegisterResponse.Duplicated> duplicated =
                new ArrayList<>();

        List<ImageRegisterResponse.Failed> failed =
                new ArrayList<>();

        for (ImageRegisterItemRequest image : request.images()) {
            String validationFailure = validate(image);

            if (validationFailure != null) {
                failed.add(
                        new ImageRegisterResponse.Failed(
                                fileNameOf(image),
                                validationFailure
                        )
                );
                continue;
            }

            try {
                RegistrationResult result =
                        registerInIndependentTransaction(userId, image);

                if (result.duplicated()) {
                    duplicated.add(
                            new ImageRegisterResponse.Duplicated(
                                    image.fileName(),
                                    result.imageId()
                            )
                    );
                } else {
                    registered.add(
                            new ImageRegisterResponse.Registered(
                                    result.imageId(),
                                    image.fileName(),
                                    result.presignedUrl(),
                                    PUT_URL_TTL.toSeconds()
                            )
                    );
                }
            } catch (BusinessException e) {
                failed.add(
                        new ImageRegisterResponse.Failed(
                                image.fileName(),
                                e.getErrorCode().getCode()
                        )
                );
            } catch (RuntimeException e) {
                log.error(
                        "이미지 배치 등록 실패: userId={}, fileName={}",
                        userId,
                        image.fileName(),
                        e
                );

                failed.add(
                        new ImageRegisterResponse.Failed(
                                image.fileName(),
                                "INTERNAL_ERROR"
                        )
                );
            }
        }

        return new ImageRegisterResponse(
                List.copyOf(registered),
                List.copyOf(duplicated),
                List.copyOf(failed)
        );
    }

    private RegistrationResult registerInIndependentTransaction(
            Integer userId,
            ImageRegisterItemRequest request
    ) {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        return transaction.execute(
                status -> registerOne(userId, request)
        );
    }

    private RegistrationResult registerOne(
            Integer userId,
            ImageRegisterItemRequest request
    ) {
        UserImage idempotent = userImageRepository
                .findByUserIdAndClientRequestId(
                        userId,
                        request.clientRequestId()
                )
                .orElse(null);

        if (idempotent != null) {
            return duplicated(idempotent.getImageId());
        }

        String normalizedContentHash =
                request.contentHash().toLowerCase(Locale.ROOT);

        ImageAsset existingAsset = imageAssetRepository
                .findByContentHash(normalizedContentHash)
                .orElse(null);

        if (existingAsset != null) {
            if (!existingAsset.getFileSize().equals(request.fileSize())) {
                throw new BusinessException(
                        ImageErrorCode.DUPLICATE_IMAGE
                );
            }

            userImageRepository
                    .findByUserIdAndImageId(
                            userId,
                            existingAsset.getImageId()
                    )
                    .orElseGet(
                            () -> createUserImage(
                                    userId,
                                    request,
                                    existingAsset,
                                    OffsetDateTime.now()
                            )
                    );

            return duplicated(existingAsset.getImageId());
        }

        Integer imageId = imageAssetRepository.nextImageId();

        String s3Key = "%d/%s-%s".formatted(
                userId,
                imageId,
                request.fileName()
        );

        OffsetDateTime now = OffsetDateTime.now();

        ImageAsset imageAsset = ImageAsset.builder()
                .imageId(imageId)
                .fileName(request.fileName())
                .contentHash(normalizedContentHash)
                .fileSize(request.fileSize())
                .s3Key(s3Key)
                .uploadStatus("PENDING")
                .createdAt(now)
                .updatedAt(now)
                .build();

        imageAssetRepository.save(imageAsset);

        createUserImage(
                userId,
                request,
                imageAsset,
                now
        );

        return new RegistrationResult(
                imageId,
                false,
                createPresignedUrl(imageAsset)
        );
    }

    private UserImage createUserImage(
            Integer userId,
            ImageRegisterItemRequest request,
            ImageAsset imageAsset,
            OffsetDateTime now
    ) {
        UserImage userImage = UserImage.builder()
                .imageId(imageAsset.getImageId())
                .userId(userId)
                .clientRequestId(request.clientRequestId())
                .title(request.fileName())
                .createdAt(now)
                .updatedAt(now)
                .build();

        userImageRepository.save(userImage);

        String nickname = userRepository
                .findById(userId)
                .map(User::getNickname)
                .orElse(null);

        userImageViewRepository.upsert(
                new UserImageViewRow(
                        userId,
                        imageAsset.getImageId(),
                        nickname,
                        imageAsset.getFileName(),
                        imageAsset.getS3Key(),
                        null,
                        userImage.getTitle(),
                        null,
                        null,
                        List.of(),
                        imageAsset.getUploadStatus(),
                        userImage.getAnalysisStatus(),
                        userImage.getFavorite(),
                        now.toInstant(),
                        now.toInstant()
                )
        );

        return userImage;
    }

    private String createPresignedUrl(ImageAsset imageAsset) {
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(
                                storageProperties
                                        .getBucket()
                                        .getPictures()
                        )
                        .key(imageAsset.getS3Key())
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(PUT_URL_TTL)
                        .putObjectRequest(putObjectRequest)
                        .build();

        PresignedPutObjectRequest presigned =
                s3Presigner.presignPutObject(presignRequest);

        return presigned.url().toString();
    }

    private RegistrationResult duplicated(Integer imageId) {
        return new RegistrationResult(
                imageId,
                true,
                null
        );
    }

    private String validate(ImageRegisterItemRequest request) {
        if (request == null) {
            return "INVALID_REQUEST";
        }

        Set<ConstraintViolation<ImageRegisterItemRequest>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            if (request.ocr() == null
                    || violations.stream().anyMatch(
                    violation -> violation
                            .getPropertyPath()
                            .toString()
                            .startsWith("ocr")
            )) {
                return "OCR_REQUIRED";
            }

            if (request.contentHash() == null
                    || violations.stream().anyMatch(
                    violation -> violation
                            .getPropertyPath()
                            .toString()
                            .equals("contentHash")
            )) {
                return "INVALID_CONTENT_HASH";
            }

            if (request.fileSize() == null
                    || violations.stream().anyMatch(
                    violation -> violation
                            .getPropertyPath()
                            .toString()
                            .equals("fileSize")
            )) {
                return "INVALID_FILE_SIZE";
            }

            return "INVALID_REQUEST";
        }

        if (!isSupportedFormat(request.fileName())) {
            return "UNSUPPORTED_FORMAT";
        }

        return null;
    }

    private boolean isSupportedFormat(String fileName) {
        int extensionSeparator = fileName.lastIndexOf('.');

        if (extensionSeparator < 0
                || extensionSeparator == fileName.length() - 1) {
            return false;
        }

        String extension = fileName
                .substring(extensionSeparator + 1)
                .toLowerCase(Locale.ROOT);

        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    private String fileNameOf(
            ImageRegisterItemRequest request
    ) {
        if (request == null || request.fileName() == null) {
            return "";
        }

        return request.fileName();
    }

    private record RegistrationResult(
            Integer imageId,
            boolean duplicated,
            String presignedUrl
    ) {
    }
}