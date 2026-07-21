package com.ssafy.modera.global.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {

    @Pointcut("execution(* com.ssafy..*Controller.*(..))")
    public void controller() {}

    @Pointcut("execution(* com.ssafy..*Service.*(..))")
    public void service() {}

    @Around("controller() || service()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long totalTime = System.currentTimeMillis() - startTime;
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();

            log.info("[{}] {}.{} | Args: {} | Ret: {} | {}ms",
                    getLayer(signature),              // 1. 레이어 (API / SVC)
                    className(signature),             // 2. 클래스명
                    methodName(signature),            // 3. 메서드명
                    formatArgs(joinPoint.getArgs()),  // 4. 파라미터 (단순화)
                    formatResult(result),             // 5. 결과값 (길이 제한)
                    totalTime                         // 6. 소요시간
            );
        }
    }

    private String getLayer(MethodSignature signature) {
        String className = signature.getDeclaringType().getSimpleName();
        if (className.endsWith("Controller")) return "API";
        if (className.endsWith("Service")) return "SVC";
        return "ETC";
    }

    private String className(MethodSignature signature) {
        return signature.getDeclaringType().getSimpleName();
    }

    private String methodName(MethodSignature signature) {
        return signature.getName();
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return Arrays.toString(args);
    }

    private String formatResult(Object result) {
        if (result == null) return "void";
        String s = result.toString();
        return s.length() > 100 ? s.substring(0, 100) + "..." : s;
    }
}
