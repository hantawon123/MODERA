package com.ssafy.modera.sync.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ssafy.modera.core.data.repository.analyzedImage.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.io.IOException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val analyzedImageRepository: AnalyzedImageRepository,
    private val documentRepository: DocumentRepository,
) : CoroutineWorker(
    appContext = appContext,
    params = workerParameters,
) {

    override suspend fun doWork(): Result {
        val resource = SyncResourceType.fromServerValue(
            value = inputData.getString(KEY_RESOURCE),
        ) ?: return Result.failure()

        val resourceId = inputData.getLong(
            KEY_RESOURCE_ID,
            INVALID_RESOURCE_ID,
        )

        if (resourceId <= INVALID_RESOURCE_ID) {
            return Result.failure()
        }

        return try {
            val synced = when (resource) {
                SyncResourceType.IMAGE,
                SyncResourceType.IMAGE_CATEGORY -> {
                    analyzedImageRepository.syncWith(
                        resourceId = resourceId,
                    )
                }

                SyncResourceType.DOCUMENT -> {
                    documentRepository.syncWith(
                        resourceId = resourceId,
                    )
                }

                SyncResourceType.CALENDAR -> {
                    return Result.failure()
                }

            }

            if (synced) {
                Result.success()
            } else {
                Result.failure()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            Result.retry()
        } catch (exception: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_RESOURCE = "sync_resource"
        const val KEY_RESOURCE_ID = "sync_resource_id"

        private const val INVALID_RESOURCE_ID = -1L
    }
}