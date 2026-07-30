package com.ssafy.modera.core.common.datetime

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ModeraDateFormatter {

    private val locale = Locale.KOREA

    private val time24HourFormatter =
        DateTimeFormatter.ofPattern(
            "HH:mm",
            locale,
        )

    private val time12HourFormatter =
        DateTimeFormatter.ofPattern(
            "a h:mm",
            locale,
        )

    private val monthDayFormatter =
        DateTimeFormatter.ofPattern(
            "M월 d일",
            locale,
        )

    private val monthDayTimeFormatter =
        DateTimeFormatter.ofPattern(
            "M월 d일 a h:mm",
            locale,
        )

    private val yearMonthDayFormatter =
        DateTimeFormatter.ofPattern(
            "yyyy년 M월 d일",
            locale,
        )

    private val yearMonthDayTimeFormatter =
        DateTimeFormatter.ofPattern(
            "yyyy년 M월 d일 a h:mm",
            locale,
        )

    /**
     * 밀리초 단위 Unix timestamp를 표시 문자열로 변환합니다.
     *
     * 예:
     * 1785493380000
     */
    fun formatMillis(
        timestampMillis: Long,
        style: ModeraDateStyle = ModeraDateStyle.SMART,
        zoneId: ZoneId = ZoneId.systemDefault(),
        clock: Clock = Clock.system(zoneId),
    ): String {
        return format(
            instant = Instant.ofEpochMilli(timestampMillis),
            style = style,
            zoneId = zoneId,
            clock = clock,
        )
    }

    /**
     * 초 단위 Unix timestamp를 표시 문자열로 변환합니다.
     *
     * 예:
     * 1785493380
     */
    fun formatSeconds(
        timestampSeconds: Long,
        style: ModeraDateStyle = ModeraDateStyle.SMART,
        zoneId: ZoneId = ZoneId.systemDefault(),
        clock: Clock = Clock.system(zoneId),
    ): String {
        return format(
            instant = Instant.ofEpochSecond(timestampSeconds),
            style = style,
            zoneId = zoneId,
            clock = clock,
        )
    }

    /**
     * Instant를 표시 문자열로 변환합니다.
     */
    fun format(
        instant: Instant,
        style: ModeraDateStyle = ModeraDateStyle.SMART,
        zoneId: ZoneId = ZoneId.systemDefault(),
        clock: Clock = Clock.system(zoneId),
    ): String {
        val targetDateTime = instant.atZone(zoneId)
        val currentDateTime = Instant
            .now(clock)
            .atZone(zoneId)

        return when (style) {
            ModeraDateStyle.SMART -> {
                formatSmart(
                    targetDateTime = targetDateTime,
                    currentDateTime = currentDateTime,
                )
            }

            ModeraDateStyle.TIME_24_HOUR -> {
                targetDateTime.format(time24HourFormatter)
            }

            ModeraDateStyle.TIME_12_HOUR -> {
                targetDateTime.format(time12HourFormatter)
            }

            ModeraDateStyle.MONTH_DAY -> {
                targetDateTime.format(monthDayFormatter)
            }

            ModeraDateStyle.MONTH_DAY_TIME -> {
                targetDateTime.format(monthDayTimeFormatter)
            }

            ModeraDateStyle.YEAR_MONTH_DAY -> {
                targetDateTime.format(yearMonthDayFormatter)
            }

            ModeraDateStyle.YEAR_MONTH_DAY_TIME -> {
                targetDateTime.format(yearMonthDayTimeFormatter)
            }
        }
    }

    private fun formatSmart(
        targetDateTime: ZonedDateTime,
        currentDateTime: ZonedDateTime,
    ): String {
        val targetInstant = targetDateTime.toInstant()
        val currentInstant = currentDateTime.toInstant()

        // 미래 시각은 상대 시간으로 표시하지 않습니다.
        if (targetInstant.isAfter(currentInstant)) {
            return formatAbsolute(
                targetDateTime = targetDateTime,
                currentDateTime = currentDateTime,
            )
        }

        val elapsedSeconds = Duration
            .between(
                targetInstant,
                currentInstant,
            )
            .seconds

        return when {
            elapsedSeconds < SECONDS_PER_MINUTE -> {
                "방금"
            }

            elapsedSeconds < SECONDS_PER_HOUR -> {
                val elapsedMinutes =
                    elapsedSeconds / SECONDS_PER_MINUTE

                "${elapsedMinutes}분 전"
            }

            elapsedSeconds < RELATIVE_HOUR_LIMIT * SECONDS_PER_HOUR -> {
                val elapsedHours =
                    elapsedSeconds / SECONDS_PER_HOUR

                "${elapsedHours}시간 전"
            }

            else -> {
                formatAbsolute(
                    targetDateTime = targetDateTime,
                    currentDateTime = currentDateTime,
                )
            }
        }
    }

    private fun formatAbsolute(
        targetDateTime: ZonedDateTime,
        currentDateTime: ZonedDateTime,
    ): String {
        return when {
            targetDateTime.toLocalDate() ==
                    currentDateTime.toLocalDate() -> {
                targetDateTime.format(time12HourFormatter)
            }

            targetDateTime.year == currentDateTime.year -> {
                targetDateTime.format(monthDayFormatter)
            }

            else -> {
                targetDateTime.format(yearMonthDayFormatter)
            }
        }
    }

    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE

    /**
     * 몇 시간 전까지 상대 시간으로 표시할지 결정합니다.
     *
     * 6시간 이상 지난 오늘 데이터는 "오후 7:23"으로 표시됩니다.
     */
    private const val RELATIVE_HOUR_LIMIT = 6L
}