package com.ssafy.modera.api.domain.image.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * worker HTTP 호출이 DB 트랜잭션 밖에서 수행되는 경계를 고정한다.
 * AuthTransactionBoundaryTest와 같은 방식 — 누군가 클래스/메서드에 @Transactional을
 * 되돌려 붙이면 여기서 바로 깨진다.
 */
class ImageSimilarTransactionBoundaryTest {

    @Test
    void workerNetworkCallsStayOutsideDatabaseTransactions() throws Exception {
        assertThat(ImageSimilarService.class.getAnnotation(Transactional.class)).isNull();

        Method similar = ImageSimilarService.class
                .getMethod("getSimilarImages", Integer.class, Integer.class, int.class);
        Method documentize = ImageSimilarService.class
                .getMethod("findDocumentizeCandidates", Integer.class, List.class);

        assertThat(similar.getAnnotation(Transactional.class)).isNull();
        assertThat(documentize.getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void databaseReadsKeepTheirOwnShortReadOnlyTransactions() throws Exception {
        Transactional baseTitle = ImageSimilarReader.class
                .getMethod("readBaseTitle", Integer.class, Integer.class)
                .getAnnotation(Transactional.class);
        Transactional summaries = ImageSimilarReader.class
                .getMethod("readSummaries", Integer.class, List.class)
                .getAnnotation(Transactional.class);
        Transactional documentizeBase = ImageSimilarReader.class
                .getMethod("validateDocumentizeBase", Integer.class, List.class)
                .getAnnotation(Transactional.class);

        assertThat(baseTitle).isNotNull();
        assertThat(baseTitle.readOnly()).isTrue();
        assertThat(summaries).isNotNull();
        assertThat(summaries.readOnly()).isTrue();
        assertThat(documentizeBase).isNotNull();
        assertThat(documentizeBase.readOnly()).isTrue();
    }
}
