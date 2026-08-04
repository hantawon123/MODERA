package com.ssafy.modera.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 문서와 문서를 구성하는 분석 이미지 ID의 관계입니다.
 */
@Entity(
    tableName = "document_image_cross_refs",
    primaryKeys = [
        "documentId",
        "imageId",
    ],
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["documentId"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["documentId"]),
        Index(value = ["imageId"]),
    ],
)
data class DocumentImageCrossRef(
    val documentId: Long,
    val imageId: Long,
    val position: Int,
)