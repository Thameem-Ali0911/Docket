package com.docket.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the executor backing all @Async methods (document OCR/extraction today).
 *
 * Two problems this fixes vs. Spring Boot's default:
 *  1. Without this, @Async falls back to SimpleAsyncTaskExecutor, which spins up a brand
 *     new unbounded OS thread per task - a burst of uploads can exhaust system threads.
 *     A bounded ThreadPoolTaskExecutor with a queue and a sane rejection policy is safer.
 *  2. @Async void methods return no Future, so if an exception/error ever still manages to
 *     escape a method's own try/catch, Spring normally just logs it via a generic internal
 *     logger with no context. getAsyncUncaughtExceptionHandler below is the last line of
 *     defense: it guarantees we always log which method/args were involved instead of the
 *     failure disappearing without a trace.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("docket-async-");
        // CallerRunsPolicy: if the queue is full, run the task on the calling thread instead
        // of silently dropping it - backpressure instead of data loss.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... params) -> {
            log.error("Uncaught exception in @Async method '{}'. Params: {}. This should not "
                + "happen if the method has its own try/catch - if you see this, add error "
                + "handling to that method instead of relying on this fallback.",
                method.getName(), params, throwable);
        };
    }
}
