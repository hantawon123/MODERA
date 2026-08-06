package com.ssafy.modera.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ssafy.modera.core.database.dao.AnalyzedImageDao
import com.ssafy.modera.core.database.dao.CategoryDao
import com.ssafy.modera.core.database.dao.DocumentDao
import com.ssafy.modera.core.database.model.AnalyzedImageEntity
import com.ssafy.modera.core.database.model.CategoryEntity
import com.ssafy.modera.core.database.model.DocumentEntity
import com.ssafy.modera.core.database.model.DocumentImageCrossRef
import com.ssafy.modera.core.database.util.StringListConverter

@Database(
    entities = [
        DocumentEntity::class,
        DocumentImageCrossRef::class,
        CategoryEntity::class,
        AnalyzedImageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(
    StringListConverter::class,
)
internal abstract class ModeraDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun analyzedImageDao(): AnalyzedImageDao
    abstract fun categoryDao(): CategoryDao
}