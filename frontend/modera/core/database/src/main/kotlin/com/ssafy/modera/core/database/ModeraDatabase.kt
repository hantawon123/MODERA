package com.ssafy.modera.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ssafy.modera.core.database.dao.DocumentDao
import com.ssafy.modera.core.database.model.DocumentEntity
import com.ssafy.modera.core.database.model.DocumentImageCrossRef

@Database(
    entities = [
        DocumentEntity::class,
        DocumentImageCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class ModeraDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
}