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
        // Sports with a "game" wrapper (hockey, rugby, NFL)
        private GameInfo game;

        // Sports with flat structure (basketball, baseball, handball, volleyball)
        private Long id;
        private String date;
        private StatusInfo status;

        private LeagueInfo league;
        private TeamsInfo teams;
        private ScoresInfo scores;

        @com.fasterxml.jackson.annotation.JsonIgnore
        public Long getEffectiveId() {
            return game != null && game.getId() != null ? game.getId() : id;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public String getEffectiveDate() {
            return game != null && game.getDate() != null ? game.getDate() : date;
        }

        @com.fasterxml.jackson.annotation.JsonIgnore
        public StatusInfo getEffectiveStatus() {
            return game != null && game.getStatus() != null ? game.getStatus() : status;
        }
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
        private Integer total;    // basketball, baseball, hockey, volleyball (sets won)
        private Integer fulltime; // handball

        /** Returns whichever score field is populated. */
        @com.fasterxml.jackson.annotation.JsonIgnore
        public Integer getEffectiveTotal() {
            return total != null ? total : fulltime;
        }
    }
}
