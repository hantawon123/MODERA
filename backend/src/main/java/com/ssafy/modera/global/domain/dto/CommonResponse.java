package com.ssafy.modera.global.domain.dto;

import com.ssafy.modera.global.domain.ErrorCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.List;

@JsonPropertyOrder({"result", "code", "message", "data", "timestamp"})
public record CommonResponse<T>(
        Result result,
        String code,
        String message,
        T data,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {

    public enum Result {
        SUCCESS, FAIL
    }

    public static <T> CommonResponse<T> onSuccess(T data) {
        return new CommonResponse<>(Result.SUCCESS, "200", "요청이 성공적으로 처리되었습니다.", data, LocalDateTime.now());
    }

    public static CommonResponse<Void> onSuccess() {
        return new CommonResponse<>(Result.SUCCESS, "200", "요청이 성공적으로 처리되었습니다.", null, LocalDateTime.now());
    }

    public static CommonResponse<Void> onFailure(ErrorCode errorCode) {
        return new CommonResponse<>(Result.FAIL, errorCode.getCode(), errorCode.getMessage(), null, LocalDateTime.now());
    }

    public static <T> CommonResponse<T> onFailure(ErrorCode errorCode, T data) {
        return new CommonResponse<>(Result.FAIL, errorCode.getCode(), errorCode.getMessage(), data, LocalDateTime.now());
    }

    public static CommonResponse<List<ValidationErrorDetail>> onFailure(ErrorCode errorCode, BindingResult bindingResult) {
        return new CommonResponse<>(
                Result.FAIL,
                errorCode.getCode(),
                errorCode.getMessage(),
                ValidationErrorDetail.of(bindingResult),
                LocalDateTime.now()
        );
    }

    public record ValidationErrorDetail(
            String field,
            String reason
    ) {
        public static List<ValidationErrorDetail> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(error -> new ValidationErrorDetail(
                            error.getField(),
                            error.getDefaultMessage()
                    ))
                    .toList();
        }
    }
}
