package com.ssafy.modera.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

    @Upsert
    suspend fun upsertDocument(
        entity: DocumentEntity,
    )

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
}