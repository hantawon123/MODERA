package com.ssafy.modera.api.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 이미지 분석에서 추출된 일정 원본. 시작·종료 시각은 분석이 추출하지 못하면 null이다.
 */
@Entity
@Table(name = "schedule", schema = "schedule_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    @Builder
    public Schedule(String title, OffsetDateTime startAt, OffsetDateTime endAt, OffsetDateTime updatedAt) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = updatedAt;
    }

    public void softDelete(OffsetDateTime now) {
        this.delYn = "Y";
        this.updatedAt = now;
    }
}
