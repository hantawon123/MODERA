package com.ssafy.modera.sync.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncWorkEnqueuer @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun enqueue(
        resource: SyncResourceType,
        resourceId: Long,
    ) {
        require(resourceId > 0L) {
            "resourceId must be greater than 0."
        }

        val inputData = workDataOf(
            SyncWorker.KEY_RESOURCE to resource.name,
            SyncWorker.KEY_RESOURCE_ID to resourceId,
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_SECONDS,
                    TimeUnit.SECONDS,
                )
                .addTag(SYNC_WORK_TAG)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                uniqueWorkName(
                    resource = resource,
                    resourceId = resourceId,
                ),
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
    }

    private fun uniqueWorkName(
        resource: SyncResourceType,
        resourceId: Long,
    ): String =
        "$SYNC_WORK_TAG:${resource.name}:$resourceId"

    private companion object {
        const val SYNC_WORK_TAG = "resource_sync"
        const val BACKOFF_DELAY_SECONDS = 10L
    }
}