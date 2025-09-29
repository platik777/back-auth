package ru.platik777.backauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Конфигурация для планировщика задач
 * Используется для очистки просроченных токенов и API ключей
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Конфигурация для @Scheduled аннотаций
}