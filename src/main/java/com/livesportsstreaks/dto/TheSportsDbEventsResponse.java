package com.livesportsstreaks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TheSportsDbEventsResponse {

    private List<Event> events;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String idEvent;
        private String strHomeTeam;
        private String strAwayTeam;
        private String intHomeScore;   // null for upcoming, string integer for completed/live
        private String intAwayScore;
        private String strStatus;      // "Match Finished", "1H", "2H", "HT", "ET", "", "NS", etc.
        private String strTimestamp;   // ISO-8601, e.g. "2024-01-15T19:00:00+00:00"
        private String dateEvent;      // "YYYY-MM-DD" fallback
        private String strTime;        // "HH:MM:SS" fallback
        private String strLeague;
        private String strSport;
    }
}
