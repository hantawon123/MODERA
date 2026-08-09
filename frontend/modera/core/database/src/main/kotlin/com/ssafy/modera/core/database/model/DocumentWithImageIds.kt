package com.ssafy.modera.core.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.ssafy.modera.core.model.DocumentDetail

data class DocumentWithImageIds(
    @Embedded
    val document: DocumentEntity,

    @Relation(
        parentColumn = "documentId",
        entityColumn = "documentId",
        entity = DocumentImageCrossRef::class,
    )
    val imageCrossRefs: List<DocumentImageCrossRef>,
)

fun DocumentWithImageIds.asExternalModel(): DocumentDetail =
    document.asDocumentDetail(
        imageIds = imageCrossRefs
            .sortedBy(DocumentImageCrossRef::position)
            .map(DocumentImageCrossRef::imageId),
    )