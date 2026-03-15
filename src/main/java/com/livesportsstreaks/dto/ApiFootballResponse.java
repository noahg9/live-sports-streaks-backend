package com.livesportsstreaks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFootballResponse {

    private List<Fixture> response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private FixtureInfo fixture;
        private LeagueInfo league;
        private TeamsInfo teams;
        private GoalsInfo goals;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixtureInfo {
        private Long id;
        private String date;   // ISO-8601 string, e.g. "2024-11-01T19:00:00+00:00"
        private StatusInfo status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusInfo {
        @JsonProperty("short")
        private String shortStatus;  // "FT", "1H", "HT", "2H", "ET", "NS", etc.
        @JsonProperty("long")
        private String longStatus;
        private Integer elapsed;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeagueInfo {
        private Long id;
        private String name;
        private String country;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamsInfo {
        private TeamInfo home;
        private TeamInfo away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamInfo {
        private Long id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoalsInfo {
        private Integer home;
        private Integer away;
    }
}
