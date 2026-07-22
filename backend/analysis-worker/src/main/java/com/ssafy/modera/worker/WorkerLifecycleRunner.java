package com.ssafy.modera.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * web 서버가 없는 프로세스가 기동 직후 종료되지 않도록 메인 스레드를 대기시킨다.
 * 6단계에서 Redis Streams 리스너(XREADGROUP 블로킹 루프)가 이 역할을 대신하면 제거한다.
 */
@Slf4j
@Component
public class WorkerLifecycleRunner implements CommandLineRunner {

    private final CountDownLatch keepAlive = new CountDownLatch(1);

    @Override
    public void run(String... args) throws InterruptedException {
        log.info("analysis-worker 기동 완료. 이벤트 리스너 구현 전까지 대기 상태로 유지된다.");
        keepAlive.await();
    }
}
