package com.ssafy.modera.api.global.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * /api/v1/** 컨트롤러에 붙이는 마커. {@code @RestControllerAdvice}는 URL 패턴으로
 * 직접 범위를 잡을 수 없어서, 이 애노테이션으로 GlobalExceptionHandler의 적용 대상을
 * 잡는다. /internal/** 컨트롤러(StorageWebhookController)에는 붙이지 않는다 —
 * 거기는 예외가 나면 Spring 기본 오류 응답을 그대로 쓴다(envelope 미적용 범위).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiV1Controller {
}
