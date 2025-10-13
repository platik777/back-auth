package ru.platik777.backauth.dto.monitoring;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetrics {
    private LocalDateTime startTime;
    private String uptime;
    private Long uptimeSeconds;
    private Long heapUsedMB;
    private Long heapMaxMB;
    private Double heapUsagePercent;
}
