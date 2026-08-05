package com.ssafy.modera.core.database.util

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class StringListConverter {

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        Json.decodeFromString(value)
}