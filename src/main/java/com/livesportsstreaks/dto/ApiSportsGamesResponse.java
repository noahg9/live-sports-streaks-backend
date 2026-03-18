package com.livesportsstreaks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiSportsGamesResponse {

    private List<GameEntry> response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameEntry {
        private GameInfo game;
        private LeagueInfo league;
        private TeamsInfo teams;
        private ScoresInfo scores;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GameInfo {
        private Long id;
        private String date;
        private StatusInfo status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusInfo {
        @JsonProperty("short")
        private String shortStatus;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LeagueInfo {
        private String name;
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
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoresInfo {
        private ScoreValue home;
        private ScoreValue away;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreValue {
        private Integer total;
    }
}
