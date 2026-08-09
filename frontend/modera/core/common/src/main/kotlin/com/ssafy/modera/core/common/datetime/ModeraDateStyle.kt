package com.ssafy.modera.core.common.datetime

/**
 * 날짜 및 시간의 표시 형식을 정의합니다.
 */
enum class ModeraDateStyle {

    /**
     * 현재 시각을 기준으로 적절한 형식을 자동으로 선택합니다.
     *
     * - 방금
     * - 34분 전
     * - 3시간 전
     * - 오후 7:23
     * - 7월 27일
     * - 2026년 7월 27일
     */
    SMART,

    /**
     * 23:59
     */
    TIME_24_HOUR,

    /**
     * 오후 7:23
     */
    TIME_12_HOUR,

    /**
     * 7월 27일
     */
    MONTH_DAY,

    /**
     * 7월 17일 오후 7:23
     */
    MONTH_DAY_TIME,

    /**
     * 2026년 7월 27일
     */
    YEAR_MONTH_DAY,

    /**
     * 2026년 7월 17일 오후 7:23
     */
    YEAR_MONTH_DAY_TIME,
}