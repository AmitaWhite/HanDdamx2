package com.white.handdam.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * {@code @Async} 작업용 스레드 풀 설정.
 *
 * <p>용도별로 풀을 분리한다. 하나를 공유하면 느린 작업(메일 발송)이 큐를 채워
 * 다른 작업(알림)까지 밀리기 때문이다.
 *
 * <p><b>거부 정책</b> — 두 풀 모두 기본 {@code AbortPolicy} 대신 로그를 남기고 버린다.
 * 기본 정책은 큐 포화 시 <b>작업 제출 시점에</b> {@code TaskRejectedException} 을 던지는데,
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 와 조합되면 이 예외가
 * {@code afterCommit} 콜백 밖으로 나가 <b>커밋을 호출한 원본 요청까지 전파된다</b>
 * ({@code AbstractPlatformTransactionManager.processCommit} 이 afterCommit 예외를 잡지 않는다).
 *
 * <p>그 결과 채팅 메시지는 이미 저장·전송이 끝났는데도 API 가 500 을 반환하고,
 * 클라이언트가 재시도하면 메시지가 중복된다. 부가 기능인 알림 때문에 본 기능이 실패하면 안 되므로
 * 제출 실패는 로그만 남기고 넘어간다.
 *
 * <p>{@code DiscardPolicy} 대신 직접 핸들러를 두는 이유는 유실을 조용히 넘기지 않기 위해서다.
 * {@code CallerRunsPolicy} 는 이미 포화된 시점에 요청 스레드로 DB 쓰기와 네트워크 전송을
 * 떠넘기게 되므로 쓰지 않는다.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /** 셧다운 시 진행 중인 작업을 기다리는 최대 시간(초) */
    private static final int AWAIT_TERMINATION_SECONDS = 10;

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(logAndDiscard("email"));
        // 셧다운 시 큐에 남은 작업이 거부 핸들러를 타지 않도록 대기시킨다
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(logAndDiscard("notification"));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }

    /**
     * 큐 포화 시 작업을 버리되 로그는 남기는 거부 핸들러.
     *
     * <p>반드시 {@code initialize()} 호출 전에 설정해야 적용된다.
     *
     * @param poolName 로그에 남길 풀 이름
     */
    private static RejectedExecutionHandler logAndDiscard(String poolName) {
        return (runnable, executor) -> log.warn(
                "{} 풀 포화로 작업을 버립니다 - activeCount={}, poolSize={}, queueSize={}",
                poolName,
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size()
        );
    }
}
