package com.ssafy.modera.core.data.repository

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
import com.ssafy.modera.core.network.service.AnalyzedImageClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.ssafy.modera.core.network.model.analyzedimage.asExternalModel as asNetworkExternalModel

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
        analyzedImageDao
            .getAnalyzedImageWithCategory(
                imageId = imageId,
            )
            .filterNotNull()
            .map { analyzedImage ->
                analyzedImage.asExternalModel()
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

    override fun getRelatedImages(
        imageId: Long,
    ): Flow<List<AnalyzedImage>> = flow {
        val relatedImages = analyzedImageClient
            .fetchRelatedImages(
                imageId = imageId,
            )
            .map { response ->
                response.asNetworkExternalModel()
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
                response.asNetworkExternalModel()
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

    private companion object {
        const val PAGE_SIZE = 20
    }
}