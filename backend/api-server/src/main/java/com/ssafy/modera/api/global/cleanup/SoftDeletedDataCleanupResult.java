package com.ssafy.modera.api.global.cleanup;

public record SoftDeletedDataCleanupResult(
        int deletedCount,
        boolean mayHaveMore
) {
}
