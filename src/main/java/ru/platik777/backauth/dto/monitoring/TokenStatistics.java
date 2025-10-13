package ru.platik777.backauth.dto.monitoring;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenStatistics {
    private Integer blacklistedTokens;
    private Long activeBlacklistedTokens;
    private Long expiredBlacklistedTokens;
    private Long tokenLifetimeMinutes;
}
