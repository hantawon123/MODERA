package com.ssafy.modera.api.domain.schedule.controller;

import com.ssafy.modera.api.domain.schedule.dto.request.ScheduleCalendarRequest;
import com.ssafy.modera.api.domain.schedule.dto.response.ScheduleCalendarResponse;
import com.ssafy.modera.api.domain.schedule.dto.response.ScheduleDeleteResponse;
import com.ssafy.modera.api.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modera.api.domain.schedule.service.ScheduleCommandService;
import com.ssafy.modera.api.domain.schedule.service.ScheduleQueryService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import com.ssafy.modera.api.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "일정", description = "이미지 분석에서 추출된 일정 후보 조회·삭제·캘린더 등록 상태 변경")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ScheduleController {

    private final ScheduleQueryService scheduleQueryService;
    private final ScheduleCommandService scheduleCommandService;

    @Operation(
            summary = "내 일정 후보·캘린더 일정 목록 조회",
            description = """
                    분석에서 추출된 일정 후보(calendared=false)와 캘린더에 등록한 일정
                    (calendared=true)을 조회한다. 캘린더는 별도 저장소 없이 이 등록 상태
                    플래그로만 표현된다.

                    from/to는 일정 기간 [startAt, endAt]과의 겹침(경계 포함) 기준이다.
                    한쪽 시각만 있는 일정은 있는 시각을 기간으로 보고, startAt·endAt이
                    모두 null인 일정(날짜 추출 실패 후보)은 from/to를 지정하면 제외되므로
                    기간 없이 조회해야 한다. 정렬 시 시각이 null인 일정은 항상 뒤에 오고,
                    같은 시각은 scheduleId 오름차순으로 순서를 고정한다.

                    출처 이미지가 삭제된 뒤에도 일정은 유지되므로 imageId가 삭제된
                    이미지를 가리킬 수 있다(이미지 단건 조회 시 IMAGE_NOT_FOUND 가능).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "page/size 범위 초과, 지원하지 않는 sort, from/to 형식 오류 또는 from>to(INVALID_PARAMETER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ScheduleSummaryResponse>>> getSchedules(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "true는 캘린더 등록 일정, false는 미등록 후보, 미지정 시 전체")
            @RequestParam(required = false) Boolean calendared,
            @Parameter(description = "조회 시작 시각(ISO-8601 UTC), 선택")
            @RequestParam(required = false) String from,
            @Parameter(description = "조회 종료 시각(ISO-8601 UTC), 선택")
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "START_ASC 또는 START_DESC")
            @RequestParam(defaultValue = "START_ASC") String sort
    ) {
        PageResponse<ScheduleSummaryResponse> response = scheduleQueryService.getSchedules(
                userId, calendared, from, to, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success("S201", response));
    }

    @Operation(
            summary = "일정 삭제",
            description = """
                    일정 원본, 사용자 관계, 이미지-일정 관계와 조회 모델을 하나의
                    트랜잭션에서 soft-delete한다. 출처 이미지는 삭제하지 않는다.
                    본인 일정이 아니거나 이미 삭제된 일정은 404(SCHEDULE_NOT_FOUND)다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정이 없거나 요청 사용자 소유가 아니거나 이미 삭제됨(SCHEDULE_NOT_FOUND)")
    })
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDeleteResponse>> deleteSchedule(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "삭제할 일정 ID") @PathVariable Integer scheduleId
    ) {
        ScheduleDeleteResponse response = scheduleCommandService.delete(userId, scheduleId);
        return ResponseEntity.ok(ApiResponse.success("S202", "일정이 삭제되었습니다.", response));
    }

    @Operation(
            summary = "캘린더 등록 상태 변경",
            description = """
                    분석에서 추출된 일정의 사용자 캘린더 등록 여부를 변경한다.
                    같은 값으로 다시 호출해도 성공이며(멱등) updatedAt만 갱신된다.
                    본인 일정이 아니거나 이미 삭제된 일정은 404(SCHEDULE_NOT_FOUND)다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "calendared 누락(INVALID_PARAMETER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일정이 없거나 요청 사용자 소유가 아니거나 이미 삭제됨(SCHEDULE_NOT_FOUND)")
    })
    @PutMapping("/{scheduleId}/calendar")
    public ResponseEntity<ApiResponse<ScheduleCalendarResponse>> changeCalendar(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "대상 일정 ID") @PathVariable Integer scheduleId,
            @RequestBody @Valid ScheduleCalendarRequest request
    ) {
        ScheduleCalendarResponse response = scheduleCommandService.changeCalendar(
                userId, scheduleId, request.calendared());
        return ResponseEntity.ok(ApiResponse.success("S203", "캘린더 등록 상태가 변경되었습니다.", response));
    }
}
