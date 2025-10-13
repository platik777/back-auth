package ru.platik777.backauth.dto;

public record BlacklistStats(
        int totalTokens,
        long activeTokens,
        long expiredTokens,
        long tokenLifetimeMinutes
) {}
