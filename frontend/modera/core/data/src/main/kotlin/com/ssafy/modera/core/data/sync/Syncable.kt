package com.ssafy.modera.core.data.sync

/**
 * 서버의 특정 리소스를 조회해 로컬 데이터베이스와 동기화하는 계약입니다.
 */
interface Syncable {

    /**
     * [resourceId]에 해당하는 최신 데이터를 서버에서 조회해
     * 로컬 데이터베이스에 반영합니다.
     *
     * @return 동기화 성공 여부
     */
    suspend fun syncWith(
        resourceId: Long,
    ): Boolean
}