package ru.platik777.backauth.dto.monitoring;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringReport {
    private LocalDateTime timestamp;
    private SystemMetrics system;
    private TokenStatistics tokens;
    private UserStatistics users;
    private Map<String, String> services;
}