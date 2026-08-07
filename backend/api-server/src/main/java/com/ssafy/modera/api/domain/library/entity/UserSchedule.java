package com.ssafy.modera.api.domain.library.entity;

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
 * Logical relationship between a user object and a schedule object.
 * Cross-schema physical foreign keys are intentionally not used.
 * 사용자 캘린더 등록 상태({@code is_calendared_yn})의 원본이다.
 */
@Entity
@Table(name = "user_schedule", schema = "library_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_schedule_id")
    private Integer userScheduleId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "schedule_id", nullable = false)
    private Integer scheduleId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "is_calendared_yn", nullable = false, length = 1)
    private String isCalendaredYn = "N";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    @Builder
    public UserSchedule(Integer userId, Integer scheduleId, OffsetDateTime updatedAt) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.updatedAt = updatedAt;
    }

    public boolean isCalendared() {
        return "Y".equals(isCalendaredYn);
    }

    public void changeCalendared(boolean calendared, OffsetDateTime now) {
        this.isCalendaredYn = calendared ? "Y" : "N";
        this.updatedAt = now;
    }

    public void softDelete(OffsetDateTime now) {
        this.delYn = "Y";
        this.updatedAt = now;
    }
}
