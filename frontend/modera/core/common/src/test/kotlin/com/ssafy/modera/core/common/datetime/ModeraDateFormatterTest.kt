package com.ssafy.modera.core.common.datetime

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ModeraDateFormatterTest {

    private val zoneId = ZoneId.of("Asia/Seoul")

    // 2026년 7월 30일 오후 1:00
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-07-30T04:00:00Z"),
        zoneId,
    )

    @Test
    fun `1분 미만이면 방금으로 표시한다`() {
        val target = Instant.parse("2026-07-30T03:59:30Z")

        val actual = ModeraDateFormatter.format(
            instant = target,
            style = ModeraDateStyle.SMART,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("방금", actual)
    }

    @Test
    fun `1시간 미만이면 분 전으로 표시한다`() {
        val target = Instant.parse("2026-07-30T03:26:00Z")

        val actual = ModeraDateFormatter.format(
            instant = target,
            style = ModeraDateStyle.SMART,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("34분 전", actual)
    }

    @Test
    fun `6시간 미만이면 시간 전으로 표시한다`() {
        val target = Instant.parse("2026-07-30T01:00:00Z")

        val actual = ModeraDateFormatter.format(
            instant = target,
            style = ModeraDateStyle.SMART,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("3시간 전", actual)
    }

    @Test
    fun `6시간 이상이면서 오늘이면 시간을 표시한다`() {
        // 한국 시간 2026년 7월 30일 오전 7:00
        val target = Instant.parse("2026-07-29T22:00:00Z")

        val actual = ModeraDateFormatter.format(
            instant = target,
            style = ModeraDateStyle.SMART,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("오전 7:00", actual)
    }

    @Test
    fun `같은 연도의 이전 날짜이면 월과 일을 표시한다`() {
        val target = Instant.parse("2026-07-27T10:23:00Z")

        val actual = ModeraDateFormatter.format(
            instant = target,
            style = ModeraDateStyle.SMART,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("7월 27일", actual)
    }

    @Test
    fun `다른 연도이면 연월일을 표시한다`() {
        val target = Instant.parse("2025-07-27T10:23:00Z")

        val actual = ModeraDateFormatter.format(
            instant = target,
            style = ModeraDateStyle.SMART,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("2025년 7월 27일", actual)
    }

    @Test
    fun `미래 시각이면 상대 시간이 아닌 절대 시간으로 표시한다`() {
        // 한국 시간 2026년 7월 30일 오후 2:00
        val target = Instant.parse("2026-07-30T05:00:00Z")

        val actual = ModeraDateFormatter.format(
            instant = target,
            style = ModeraDateStyle.SMART,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("오후 2:00", actual)
    }

    @Test
    fun `밀리초 timestamp를 지정한 형식으로 표시한다`() {
        val timestampMillis =
            Instant.parse("2026-07-17T10:23:00Z").toEpochMilli()

        val actual = ModeraDateFormatter.formatMillis(
            timestampMillis = timestampMillis,
            style = ModeraDateStyle.YEAR_MONTH_DAY_TIME,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals("2026년 7월 17일 오후 7:23", actual)
    }

    @Test
    fun `초와 밀리초 timestamp의 결과가 동일하다`() {
        val instant = Instant.parse("2026-07-17T10:23:00Z")

        val secondsResult = ModeraDateFormatter.formatSeconds(
            timestampSeconds = instant.epochSecond,
            style = ModeraDateStyle.MONTH_DAY_TIME,
            zoneId = zoneId,
            clock = fixedClock,
        )

        val millisResult = ModeraDateFormatter.formatMillis(
            timestampMillis = instant.toEpochMilli(),
            style = ModeraDateStyle.MONTH_DAY_TIME,
            zoneId = zoneId,
            clock = fixedClock,
        )

        assertEquals(secondsResult, millisResult)
    }
}