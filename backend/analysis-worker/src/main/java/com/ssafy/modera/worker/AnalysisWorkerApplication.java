package com.ssafy.modera.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// StuckJobScanner의 @Scheduled를 동작시킨다. 이 애너테이션이 없으면 스케줄러 자체가
// 등록되지 않아 배치가 조용히 실행되지 않는다(기동 에러도 나지 않는다).
@EnableScheduling
@SpringBootApplication
public class AnalysisWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalysisWorkerApplication.class, args);
    }
}
