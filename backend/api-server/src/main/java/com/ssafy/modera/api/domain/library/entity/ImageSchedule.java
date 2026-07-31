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

/**
 * Logical relationship between an image object and a schedule object.
 * Cross-schema physical foreign keys are intentionally not used.
 * 이미지 삭제(5-3)는 이 관계만 soft delete하고 일정 원본은 남긴다.
 */
@Entity
@Table(name = "image_schedule", schema = "library_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_schedule_id")
    private Integer imageScheduleId;

    @Column(name = "image_id", nullable = false)
    private Integer imageId;

    @Column(name = "schedule_id", nullable = false)
    private Integer scheduleId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    @Builder
    public ImageSchedule(Integer imageId, Integer scheduleId) {
        this.imageId = imageId;
        this.scheduleId = scheduleId;
    }

    public void softDelete() {
        this.delYn = "Y";
    }
}
