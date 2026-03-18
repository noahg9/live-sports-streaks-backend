package com.livesportsstreaks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StreakResponse(
        @JsonProperty("entity_type") String entityType,
        String name,
        String sport,
        String league,
        @JsonProperty("streak_type") String streakType,
        int length
) {}
