package com.ssafy.modera.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.ssafy.modera.core.datastore.proto.CategoriesCache
import java.io.InputStream
import java.io.OutputStream

object CategoriesCacheSerializer : Serializer<CategoriesCache> {
    override val defaultValue: CategoriesCache = CategoriesCache.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CategoriesCache {
        try {
            return CategoriesCache.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read CategoriesCache proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: CategoriesCache,
        output: OutputStream,
    ) = t.writeTo(output)
}
