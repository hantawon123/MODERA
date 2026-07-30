package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.repository.CategoryCommandRepository;
import com.ssafy.modera.api.domain.category.repository.CategoryReanalysisTarget;
import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.CategoryReanalysisCompletedPayload;
import com.ssafy.modera.contract.payload.CategoryReanalysisRequestedPayload;
import com.ssafy.modera.contract.payload.InitialCategoryResolvedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceTest {

    @Mock CategoryCommandRepository categoryCommandRepository;
    @Mock ImageQueryRepository imageQueryRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks CategoryCommandService categoryCommandService;

    @Test
    void publishesOnlyTheUsersLastFiveCategories() {
        when(categoryCommandRepository.prepareRequest(eq(7), eq(18), any(UUID.class)))
                .thenReturn(Optional.of(new CategoryReanalysisTarget(
                        31, List.of(5, 7, 8, 9, 11))));

        var response = categoryCommandService.request(7, 18);

        ArgumentCaptor<CategoryReanalysisRequestedPayload> payload =
                ArgumentCaptor.forClass(CategoryReanalysisRequestedPayload.class);
        verify(eventPublisher).publish(
                eq(Streams.IMAGE_ANALYSIS),
                eq(EventTypes.CATEGORY_REANALYSIS_REQUESTED),
                eq(1),
                payload.capture());
        assertThat(payload.getValue().categoryRequestId())
                .isEqualTo(response.categoryRequestId());
        assertThat(payload.getValue().userId()).isEqualTo(7);
        assertThat(payload.getValue().imageId()).isEqualTo(18);
        assertThat(payload.getValue().excludedCategoryIds())
                .containsExactly(5, 7, 8, 9, 11);
        assertThat(response.status()).isEqualTo("QUEUED");
    }

    @Test
    void rejectsAnUnavailableOrAlreadyPendingUserImage() {
        when(categoryCommandRepository.prepareRequest(eq(7), eq(18), any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryCommandService.request(7, 18))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ImageErrorCode.CATEGORY_REANALYSIS_UNAVAILABLE));
        verify(eventPublisher, never()).publish(any(), any(), any(Integer.class), any());
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
    }

    @Test
    void storesTheInitialAiOwnedCategoryThroughItsSeparateEvent() {
        var payload = new InitialCategoryResolvedPayload(18, 7, 3, "문서");

        categoryCommandService.initialize(payload);

        verify(categoryCommandRepository).saveInitialDefault(7, 18, 3, "문서");
        verify(imageQueryRepository).synchronizeUserCategories(7);
    }
}
