package com.ssafy.modera.core.data.repository.analyzedImage

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.data.mapper.asCategoryEntity
import com.ssafy.modera.core.data.mapper.asEntity
import com.ssafy.modera.core.database.dao.AnalyzedImageDao
import com.ssafy.modera.core.database.model.AnalyzedImageEntity
import com.ssafy.modera.core.database.model.asExternalModel
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImage
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageDetail
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageQuery
import com.ssafy.modera.core.model.analyzedimage.AnalyzedImageSortType
import com.ssafy.modera.core.network.model.analyzedimage.AnalyzedImageDetailResponse
import com.ssafy.modera.core.network.model.analyzedimage.asExternalModel
import com.ssafy.modera.core.network.service.AnalyzedImageClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultAnalyzedImageRepository @Inject constructor(
    private val analyzedImageClient: AnalyzedImageClient,
    private val analyzedImageDao: AnalyzedImageDao,
    @param:Dispatcher(ModeraDispatcher.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : AnalyzedImageRepository {

    override fun getAnalyzedImages(
        page: Int,
        query: AnalyzedImageQuery,
    ): Flow<List<AnalyzedImage>> =
        analyzedImageDao
            .getAnalyzedImageEntities(
                categoryId = query.categoryId,
                favorite = query.favorite,
                keyword = query.keyword
                    ?.trim()
                    ?.takeIf(String::isNotEmpty),
            )
            .map { entities ->
                entities
                    .sortedBy(
                        sortType = query.sort,
                    )
                    .map { entity ->
                        entity.asExternalModel()
                    }
            }

    override fun getAnalyzedImageDetail(
        imageId: Long,
    ): Flow<AnalyzedImageDetail> =
        combine(
            analyzedImageDao.getAnalyzedImageWithCategory(
                imageId = imageId,
            ),
            analyzedImageDao.hasRelatedDocuments(
                imageId = imageId,
            ),
        ) { imageWithCategory, hasRelatedDocuments ->
            imageWithCategory.asExternalModel(
                hasRelatedDocuments = hasRelatedDocuments,
            )
        }

    override fun getRelatedImages(
        imageId: Long,
    ): Flow<List<AnalyzedImage>> = flow {
        val relatedImages = analyzedImageClient
            .fetchRelatedImages(
                imageId = imageId,
            )
            .map { response ->
                response.asExternalModel()
            }

        emit(relatedImages)
    }.flowOn(ioDispatcher)

    override fun getDocumentRecommendedImages(
        selectedImageIds: List<Long>,
    ): Flow<List<AnalyzedImage>> = flow {
        val recommendedImages = analyzedImageClient
            .fetchDocumentRelatedImages(
                imageIds = selectedImageIds,
            )
            .map { response ->
                response.asExternalModel()
            }

        emit(recommendedImages)
    }.flowOn(ioDispatcher)

    override fun setAnalyzedImageFavorite(
        imageId: Long,
        favorite: Boolean,
    ): Flow<Unit> = flow {
        analyzedImageClient.updateAnalyzedImageFavorite(
            imageId = imageId,
            favorite = favorite,
        )

        analyzedImageDao.updateFavorite(
            imageId = imageId,
            favorite = favorite,
        )

        emit(Unit)
    }.flowOn(ioDispatcher)

    override fun setAnalyzedImageCalendared(
        imageId: Long,
        isCalendared: Boolean,
    ): Flow<Unit> = flow {
        updateCalendaredOrSync(
            imageId = imageId,
            isCalendared = isCalendared,
        )
        emit(Unit)
    }.flowOn(ioDispatcher)

    override fun reanalyzeImage(
        imageId: Long,
    ): Flow<Unit> = flow {
        analyzedImageClient.requestImageReanalysis(
            imageId = imageId,
        )

        emit(Unit)
    }.flowOn(ioDispatcher)

    override fun deleteAnalyzedImage(
        imageId: Long,
    ): Flow<Unit> = flow {
        analyzedImageClient.deleteAnalyzedImage(
            imageId = listOf(imageId),
        )

        analyzedImageDao.deleteAnalyzedImage(
            imageId = imageId,
        )

        emit(Unit)
    }.flowOn(ioDispatcher)

    private suspend fun updateCalendaredOrSync(
        imageId: Long,
        isCalendared: Boolean,
    ) {
        val updatedRows = analyzedImageDao.updateCalendared(
            imageId = imageId,
            isCalendared = isCalendared,
        )

        if (updatedRows > 0) {
            return
        }

        val response = analyzedImageClient.fetchAnalyzedImageDetail(
            imageId = imageId,
        )

        analyzedImageDao.upsertAnalyzedImageWithCategory(
            categoryEntity = response.asCategoryEntity(isNew = false),
            analyzedImageEntity = response.asEntity().copy(
                isCalendared = isCalendared,
            ),
        )
    }

    override suspend fun syncWith(
        resourceId: Long,
    ): Boolean = withContext(ioDispatcher) {
        val response = analyzedImageClient.fetchAnalyzedImageDetail(
            imageId = resourceId,
        )

        analyzedImageDao.upsertAnalyzedImageWithCategory(
            categoryEntity = response.asCategoryEntity(),
            analyzedImageEntity = response.asEntity(),
        )

        true
    }

    override suspend fun refreshAnalyzedImagesIfEmpty() {
        withContext(ioDispatcher) {
            if (analyzedImageDao.getAnalyzedImageCount() > 0) {
                return@withContext
            }

            val responses = analyzedImageClient.fetchAnalyzedImageDetails()
            if (responses.isEmpty()) {
                return@withContext
            }

            val categoryEntities = responses
                .distinctBy(AnalyzedImageDetailResponse::categoryId)
                .map { response ->
                    response.asCategoryEntity(isNew = false)
                }

            val imageEntities = responses.map { response ->
                response.asEntity()
            }

            analyzedImageDao.upsertAnalyzedImagesWithCategories(
                categoryEntities = categoryEntities,
                analyzedImageEntities = imageEntities,
            )
        }
    }

    private fun List<AnalyzedImageEntity>.sortedBy(
        sortType: AnalyzedImageSortType,
    ): List<AnalyzedImageEntity> =
        when (sortType) {
            AnalyzedImageSortType.UPLOADED_DESC ->
                sortedByDescending(AnalyzedImageEntity::updatedAt)

            AnalyzedImageSortType.UPLOADED_ASC ->
                sortedBy(AnalyzedImageEntity::updatedAt)

            AnalyzedImageSortType.TITLE_ASC ->
                sortedBy(AnalyzedImageEntity::title)
        }
}