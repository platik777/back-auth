package ru.platik777.backauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Конфигурация Spring Scheduling
 *
 * Обеспечивает работу аннотации @Scheduled в приложении.
 * Используется в TokenBlacklistService для периодической очистки токенов.
 *
 * Настройки:
 * - Пул потоков размером 2 (достаточно для фоновых задач)
 * - Graceful shutdown с ожиданием завершения задач
 * - Логирование инициализации
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    /**
     * Настройка планировщика задач
     *
     * Создает ThreadPoolTaskScheduler для выполнения @Scheduled методов.
     * Использование пула потоков предотвращает блокировку основного потока.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // Размер пула потоков
        // 2 потока достаточно для наших фоновых задач
        scheduler.setPoolSize(2);

        // Префикс имени потоков для удобства отладки
        scheduler.setThreadNamePrefix("scheduled-task-");

        // Graceful shutdown - ждем завершения текущих задач
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        // Инициализация планировщика
        scheduler.initialize();

        taskRegistrar.setTaskScheduler(scheduler);

        log.info("Scheduling configured: pool size = {}, thread prefix = {}",
                scheduler.getPoolSize(),
                "scheduled-task-");
    }
}