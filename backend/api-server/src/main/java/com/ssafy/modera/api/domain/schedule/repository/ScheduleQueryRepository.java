package com.ssafy.modera.api.domain.schedule.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * query_schema.user_schedule_view 조회·갱신과, 캘린더 상태 재계산에 필요한
 * library_schema 관계 조회를 담당한다. schedule_schema와의 JOIN은 schema 경계
 * 규칙 때문에 쓰지 않고, user_schedule.del_yn(원본과 같은 트랜잭션에서 함께
 * 변경됨)을 일정 활성 여부의 기준으로 삼는다.
 */
@Repository
public class ScheduleQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public ScheduleQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ScheduleListPage findSchedules(
            Integer userId,
            Boolean calendared,
            OffsetDateTime from,
            OffsetDateTime to,
            String sort,
            int page,
            int size
    ) {
        StringBuilder where = new StringBuilder("""
                FROM query_schema.user_schedule_view schedule_view
                WHERE schedule_view.user_id = ?
                  AND schedule_view.del_yn = 'N'
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);

        if (calendared != null) {
            where.append(" AND schedule_view.is_calendared_yn = ?");
            parameters.add(calendared ? "Y" : "N");
        }
        // from/to는 일정 기간 [start_at, end_at]과의 겹침(경계 포함) 기준이다.
        // 한쪽 시각만 있는 일정은 있는 시각을 기간으로 보고, 둘 다 없는 일정은
        // 기간 조건이 지정되면 제외된다(캘린더 밖 별도 목록에서 조회).
        if (from != null || to != null) {
            where.append("""
                     AND (schedule_view.start_at IS NOT NULL OR schedule_view.end_at IS NOT NULL)
                    """);
        }
        if (from != null) {
            where.append(" AND COALESCE(schedule_view.end_at, schedule_view.start_at) >= ?");
            parameters.add(from);
        }
        if (to != null) {
            where.append(" AND COALESCE(schedule_view.start_at, schedule_view.end_at) <= ?");
            parameters.add(to);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + where,
                Long.class,
                parameters.toArray()
        );

        List<Object> contentParameters = new ArrayList<>(parameters);
        contentParameters.add(size);
        contentParameters.add(page * size);
        List<ScheduleListRow> content = jdbcTemplate.query(
                """
                SELECT schedule_view.schedule_id, schedule_view.image_id, schedule_view.title,
                       schedule_view.start_at, schedule_view.end_at,
                       schedule_view.is_calendared_yn, schedule_view.updated_at
                """ + where + orderBy(sort) + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ScheduleListRow(
                        rs.getInt("schedule_id"),
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getObject("start_at", OffsetDateTime.class),
                        rs.getObject("end_at", OffsetDateTime.class),
                        "Y".equals(rs.getString("is_calendared_yn")),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                contentParameters.toArray()
        );

        return new ScheduleListPage(List.copyOf(content), total == null ? 0L : total);
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "START_DESC" ->
                    " ORDER BY schedule_view.start_at DESC NULLS LAST, schedule_view.schedule_id ASC";
            default ->
                    " ORDER BY schedule_view.start_at ASC NULLS LAST, schedule_view.schedule_id ASC";
        };
    }

    public void softDeleteView(Integer userId, Integer scheduleId, OffsetDateTime now) {
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_schedule_view
                SET del_yn = 'Y',
                    updated_at = ?
                WHERE user_id = ? AND schedule_id = ? AND del_yn = 'N'
                """,
                now,
                userId,
                scheduleId
        );
    }

    public void updateViewCalendared(Integer userId, Integer scheduleId, boolean calendared, OffsetDateTime now) {
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_schedule_view
                SET is_calendared_yn = ?,
                    updated_at = ?
                WHERE user_id = ? AND schedule_id = ? AND del_yn = 'N'
                """,
                calendared ? "Y" : "N",
                now,
                userId,
                scheduleId
        );
    }

    /**
     * 같은 이미지에 연결된, 주어진 일정을 제외한 활성 캘린더 등록 일정이 있는지 확인한다.
     * 활성 = image_schedule.del_yn='N' AND user_schedule.del_yn='N'.
     */
    public boolean existsOtherActiveCalendaredSchedule(
            Integer userId,
            Integer imageId,
            Integer excludedScheduleId
    ) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM library_schema.image_schedule image_schedule
                    JOIN library_schema.user_schedule user_schedule
                      ON user_schedule.schedule_id = image_schedule.schedule_id
                     AND user_schedule.user_id = ?
                     AND user_schedule.del_yn = 'N'
                    WHERE image_schedule.image_id = ?
                      AND image_schedule.del_yn = 'N'
                      AND image_schedule.schedule_id <> ?
                      AND user_schedule.is_calendared_yn = 'Y'
                )
                """,
                Boolean.class,
                userId,
                imageId,
                excludedScheduleId
        );
        return Boolean.TRUE.equals(exists);
    }
}
