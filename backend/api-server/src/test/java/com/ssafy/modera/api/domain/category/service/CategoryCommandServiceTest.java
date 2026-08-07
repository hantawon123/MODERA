package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.repository.CategoryCommandRepository;
import com.ssafy.modera.api.domain.category.event.CategoryReanalysisResultCoordinator;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.contract.payload.CategoryReanalysisCompletedPayload;
import com.ssafy.modera.contract.payload.InitialCategoryResolvedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceTest {

    @Mock CategoryCommandRepository categoryCommandRepository;
    @Mock ImageQueryRepository imageQueryRepository;
    @Mock CategoryReanalysisRequestDispatcher requestDispatcher;
    @Mock CategoryReanalysisResultCoordinator resultCoordinator;
    @Mock UserDataChangeOutboxService userDataChangeOutboxService;
    @InjectMocks CategoryCommandService categoryCommandService;

    @Test
    void waitsForCompletionBeforeReturning() throws Exception {
        setField(categoryCommandService, "reanalysisTimeout", Duration.ofSeconds(30));
        when(requestDispatcher.dispatch(eq(7), eq(18), any(UUID.class)))
                .thenReturn(List.of(5, 7, 8, 9, 11));
        when(resultCoordinator.await(any(UUID.class), eq(Duration.ofSeconds(30))))
                .thenAnswer(invocation -> new CategoryReanalysisCompletedPayload(
                        invocation.getArgument(0), 7, 18, 13, "새 카테고리"));

        var response = categoryCommandService.request(7, 18);

        verify(resultCoordinator).register(response.categoryRequestId());
        verify(resultCoordinator).await(response.categoryRequestId(), Duration.ofSeconds(30));
        assertThat(response.excludedCategoryIds()).containsExactly(5, 7, 8, 9, 11);
        assertThat(response.status()).isEqualTo("COMPLETED");
    }

    @Test
    void rejectsAnUnavailableOrAlreadyPendingUserImage() {
        when(requestDispatcher.dispatch(eq(7), eq(18), any(UUID.class)))
                .thenThrow(new BusinessException(ImageErrorCode.CATEGORY_REANALYSIS_UNAVAILABLE));

        assertThatThrownBy(() -> categoryCommandService.request(7, 18))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ImageErrorCode.CATEGORY_REANALYSIS_UNAVAILABLE));
    }

    @Test
    void updatesOnlyWhenTheMatchingPendingRequestIsApplied() {
        UUID requestId = UUID.randomUUID();
        var payload = new CategoryReanalysisCompletedPayload(
                requestId, 7, 18, 13, "새 카테고리");
        when(categoryCommandRepository.applyResult(
                requestId, 7, 18, 13, "새 카테고리")).thenReturn(true);

        categoryCommandService.complete(payload);

        verify(imageQueryRepository).synchronizeUserCategories(7);
        verify(userDataChangeOutboxService).record(7, "IMAGE_CATEGORY", "18");
    }

    @Test
    void storesTheInitialAiOwnedCategoryThroughItsSeparateEvent() {
        var payload = new InitialCategoryResolvedPayload(18, 7, 3, "문서", "INITIAL");

        categoryCommandService.initialize(payload);

        verify(categoryCommandRepository).saveInitialDefault(7, 18, 3, "문서");
        verify(imageQueryRepository).synchronizeUserCategories(7);
        verify(userDataChangeOutboxService).record(7, "IMAGE_UPLOAD", "18");
    }

    @Test
    void routesAReuploadCategoryToImageReanalysisResource() {
        var payload = new InitialCategoryResolvedPayload(18, 7, 3, "문서", "REUPLOAD");

        categoryCommandService.initialize(payload);

        verify(userDataChangeOutboxService).record(7, "IMAGE_REANALYSIS", "18");
    }
}
