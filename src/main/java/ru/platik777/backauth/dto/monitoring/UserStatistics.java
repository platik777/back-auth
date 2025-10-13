package ru.platik777.backauth.dto.monitoring;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatistics {
    private Long totalUsers;
    private Map<String, Long> accountTypeDistribution;
}
