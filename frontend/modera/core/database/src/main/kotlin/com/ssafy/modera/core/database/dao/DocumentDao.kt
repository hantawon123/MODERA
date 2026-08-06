package com.ssafy.modera.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssafy.modera.core.database.model.AnalyzedImageEntity
import com.ssafy.modera.core.database.model.DocumentEntity
import com.ssafy.modera.core.database.model.DocumentImageCrossRef
import com.ssafy.modera.core.database.model.DocumentWithImageIds
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query(
        """
        SELECT *
        FROM documents
        """,
    )
    fun getDocumentEntities(): Flow<List<DocumentEntity>>

    @Transaction
    @Query(
        """
        SELECT *
        FROM documents
        WHERE documentId = :documentId
        """,
    )
    fun getDocumentWithImageIds(
        documentId: Long,
    ): Flow<DocumentWithImageIds?>

    @Query(
        """
    SELECT documents.*
    FROM documents
    INNER JOIN document_image_cross_refs
        ON documents.documentId = document_image_cross_refs.documentId
    WHERE document_image_cross_refs.imageId = :imageId
    """,
    )
    fun getDocumentEntitiesByImageId(
        imageId: Long,
    ): Flow<List<DocumentEntity>>

    @Query(
        """
    SELECT analyzed_images.*
    FROM analyzed_images
    INNER JOIN document_image_cross_refs
        ON analyzed_images.imageId = document_image_cross_refs.imageId
    WHERE document_image_cross_refs.documentId = :documentId
    ORDER BY document_image_cross_refs.position ASC
    """,
    )
    fun getAnalyzedImageEntitiesByDocumentId(
        documentId: Long,
    ): Flow<List<AnalyzedImageEntity>>

    @Upsert
    suspend fun upsertDocument(
        entity: DocumentEntity,
    )

    @Query(
        """
        SELECT COUNT(*)
        FROM documents
        """,
    )
    suspend fun getDocumentCount(): Int

    @Upsert
    suspend fun upsertDocumentImageCrossRefs(
        crossRefs: List<DocumentImageCrossRef>,
    )

    @Query(
        """
        DELETE FROM document_image_cross_refs
        WHERE documentId = :documentId
        """,
    )
    suspend fun deleteDocumentImageCrossRefs(
        documentId: Long,
    )

    @Query(
        """
        DELETE FROM documents
        WHERE documentId = :documentId
        """,
    )
    suspend fun deleteDocument(
        documentId: Long,
    )

    @Transaction
    suspend fun upsertDocumentWithImageIds(
        entity: DocumentEntity,
        imageIds: List<Long>,
    ) {
        upsertDocument(entity)

        deleteDocumentImageCrossRefs(
            documentId = entity.documentId,
        )

        if (imageIds.isNotEmpty()) {
            upsertDocumentImageCrossRefs(
                crossRefs = imageIds.mapIndexed { index, imageId ->
                    DocumentImageCrossRef(
                        documentId = entity.documentId,
                        imageId = imageId,
                        position = index,
                    )
                },
            )
        }
    }

    @Transaction
    suspend fun upsertDocumentsWithImageIds(
        documents: List<Pair<DocumentEntity, List<Long>>>,
    ) {
        documents.forEach { (entity, imageIds) ->
            upsertDocumentWithImageIds(
                entity = entity,
                imageIds = imageIds,
            )
        }
    }
}