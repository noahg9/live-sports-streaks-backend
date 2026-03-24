package com.livesportsstreaks.service;

import com.livesportsstreaks.dto.TheSportsDbEventsResponse;
import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MatchFetchService {

    private static final Logger log = LoggerFactory.getLogger(MatchFetchService.class);

    // TheSportsDB status values that indicate a fully finished match
    private static final Set<String> FINISHED_STATUSES = Set.of(
            "Match Finished",
            "FT",  "AET", "PEN", "AOT", "SO", "FIN", "AP"
    );

    // TheSportsDB sport name → internal sport name + externalId offset
    // Event IDs in TheSportsDB are ~7 digits; offsets are in the billions — no overlap
    private record SportConfig(String internalName, long idOffset) {}

    private static final Map<String, SportConfig> SPORTS = Map.of(
            "Soccer",            new SportConfig("football",   0L),
            "Basketball",        new SportConfig("basketball", 10_000_000_000L),
            "Baseball",          new SportConfig("baseball",   20_000_000_000L),
            "Ice_Hockey",        new SportConfig("hockey",     30_000_000_000L),
            "Rugby_League",      new SportConfig("rugby",      40_000_000_000L),
            "Handball",          new SportConfig("handball",   50_000_000_000L),
            "Volleyball",        new SportConfig("volleyball", 60_000_000_000L),
            "American_Football", new SportConfig("nfl",        70_000_000_000L)
    );

    private final RestClient restClient;

    @Value("${api.thesportsdb.key}")
    private String apiKey;

    @Value("${api.thesportsdb.url}")
    private String baseUrl;

    public MatchFetchService() {
        this.restClient = RestClient.builder().build();
    }

    public List<Match> fetchAllLiveMatches() {
        if (!hasApiKey()) return Collections.emptyList();
        return fetchForDate(LocalDate.now().toString());
    }

    public List<Match> fetchAllRecentFinishedMatches() {
        if (!hasApiKey()) return Collections.emptyList();
        return fetchForDate(LocalDate.now().minusDays(1).toString());
    }

    /** Returns all TheSportsDB sport names this service supports, for external iteration (e.g. backfill). */
    public Set<String> getSupportedSports() {
        return SPORTS.keySet();
    }

    /** Fetches a single sport's events for the given date. No rate-limit delay — callers manage that. */
    public List<Match> fetchForSport(String date, String sportsDbSport) {
        if (!hasApiKey()) return Collections.emptyList();
        SportConfig config = SPORTS.get(sportsDbSport);
        if (config == null) return Collections.emptyList();
        return fetchEvents(date, sportsDbSport, config);
    }

    private List<Match> fetchForDate(String date) {
        List<Match> all = new ArrayList<>();
        for (Map.Entry<String, SportConfig> entry : SPORTS.entrySet()) {
            all.addAll(fetchEvents(date, entry.getKey(), entry.getValue()));
        }
        return all;
    }

    private List<Match> fetchEvents(String date, String sportsDbSport, SportConfig config) {
        String url = baseUrl + "/" + apiKey + "/eventsday.php?d=" + date + "&s=" + sportsDbSport;
        try {
            TheSportsDbEventsResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(TheSportsDbEventsResponse.class);
            if (response == null || response.getEvents() == null) return Collections.emptyList();
            return mapEvents(response.getEvents(), config.internalName(), config.idOffset());
        } catch (RestClientException e) {
            log.error("{} fetch failed [{}]: {}", sportsDbSport, url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Match> mapEvents(List<TheSportsDbEventsResponse.Event> events, String sport, long idOffset) {
        return events.stream()
                .map(event -> {
                    Team homeTeam = Team.builder()
                            .name(event.getStrHomeTeam())
                            .sport(sport)
                            .league(event.getStrLeague())
                            .build();

                    Team awayTeam = Team.builder()
                            .name(event.getStrAwayTeam())
                            .sport(sport)
                            .league(event.getStrLeague())
                            .build();

                    Long apiId = parseId(event.getIdEvent());

                    return Match.builder()
                            .externalId(apiId != null ? apiId + idOffset : null)
                            .sport(sport)
                            .date(parseDate(event))
                            .homeTeam(homeTeam)
                            .awayTeam(awayTeam)
                            .homeScore(parseScore(event.getIntHomeScore()))
                            .awayScore(parseScore(event.getIntAwayScore()))
                            .status(normalizeStatus(event.getStrStatus()))
                            .build();
                })
                .toList();
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        return FINISHED_STATUSES.contains(status) ? "FT" : status;
    }

    private Long parseId(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseScore(String score) {
        if (score == null || score.isBlank()) return null;
        try {
            return Integer.parseInt(score);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDate(TheSportsDbEventsResponse.Event event) {
        // Prefer strTimestamp (ISO-8601 with offset), fall back to dateEvent + strTime
        if (event.getStrTimestamp() != null && !event.getStrTimestamp().isBlank()) {
            try {
                return OffsetDateTime.parse(event.getStrTimestamp()).toLocalDateTime();
            } catch (Exception e) {
                log.warn("Could not parse strTimestamp '{}': {}", event.getStrTimestamp(), e.getMessage());
            }
        }
        if (event.getDateEvent() != null && !event.getDateEvent().isBlank()) {
            String time = event.getStrTime() != null && !event.getStrTime().isBlank()
                    ? event.getStrTime() : "00:00:00";
            try {
                return OffsetDateTime.parse(event.getDateEvent() + "T" + time + "+00:00").toLocalDateTime();
            } catch (Exception e) {
                log.warn("Could not parse date '{} {}': {}", event.getDateEvent(), time, e.getMessage());
            }
        }
        return null;
    }

    private boolean hasApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("api.thesportsdb.key is not configured — skipping all fetches");
            return false;
        }
        return true;
    }
}
