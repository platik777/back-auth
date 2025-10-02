package ru.platik777.backauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Конфигурация асинхронного выполнения
 * Необходима для работы @Async аннотации
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Пул потоков для асинхронных задач
     */
    @Bean(name = "asyncExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Настройка пула потоков
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-role-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Обработка отклоненных задач
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("Task rejected from async executor: {}", r.toString());
        });

        executor.initialize();

        log.info("Async executor initialized with {} core threads",
                executor.getCorePoolSize());

        return executor;
    }
}