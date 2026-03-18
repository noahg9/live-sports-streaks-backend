package com.livesportsstreaks.service;

import com.livesportsstreaks.dto.ApiFootballResponse;
import com.livesportsstreaks.dto.ApiSportsGamesResponse;
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
import java.util.Set;

@Service
public class MatchFetchService {

    private static final Logger log = LoggerFactory.getLogger(MatchFetchService.class);

    // Statuses that mean a match is fully finished across all sports
    private static final Set<String> FINISHED_STATUSES = Set.of(
            "FT",  // Full Time (football, basketball, rugby, handball, volleyball)
            "AET", // After Extra Time (football)
            "PEN", // After Penalties (football)
            "AOT", // After Overtime (basketball, hockey, handball)
            "SO",  // Shootout (hockey)
            "FIN", // Finished (baseball)
            "AP"   // After Penalties (rugby)
    );

    // Per-sport externalId offsets to prevent collisions — sport IDs are in the millions,
    // offsets are in the billions, so they never overlap
    private static final long FOOTBALL_OFFSET   = 0L;
    private static final long BASKETBALL_OFFSET = 10_000_000_000L;
    private static final long BASEBALL_OFFSET   = 20_000_000_000L;
    private static final long HOCKEY_OFFSET     = 30_000_000_000L;
    private static final long RUGBY_OFFSET      = 40_000_000_000L;
    private static final long HANDBALL_OFFSET   = 50_000_000_000L;
    private static final long VOLLEYBALL_OFFSET = 60_000_000_000L;
    private static final long NFL_OFFSET        = 70_000_000_000L;

    private final RestClient restClient;

    @Value("${api.sports.key}")     private String apiSportsKey;
    @Value("${api.football.url}")   private String footballUrl;
    @Value("${api.basketball.url}") private String basketballUrl;
    @Value("${api.baseball.url}")   private String baseballUrl;
    @Value("${api.hockey.url}")     private String hockeyUrl;
    @Value("${api.rugby.url}")      private String rugbyUrl;
    @Value("${api.handball.url}")   private String handballUrl;
    @Value("${api.volleyball.url}") private String volleyballUrl;
    @Value("${api.nfl.url}")        private String nflUrl;

    public MatchFetchService() {
        this.restClient = RestClient.builder().build();
    }

    public List<Match> fetchAllLiveMatches() {
        if (!hasApiKey()) return Collections.emptyList();
        List<Match> all = new ArrayList<>();
        all.addAll(fetchFootball(footballUrl + "/fixtures?live=all", FOOTBALL_OFFSET));
        all.addAll(fetchGames(basketballUrl + "/games?live=all", "basketball", BASKETBALL_OFFSET));
        all.addAll(fetchGames(baseballUrl + "/games?live=all", "baseball", BASEBALL_OFFSET));
        all.addAll(fetchGames(hockeyUrl + "/games?live=all", "hockey", HOCKEY_OFFSET));
        all.addAll(fetchGames(rugbyUrl + "/games?live=all", "rugby", RUGBY_OFFSET));
        all.addAll(fetchGames(handballUrl + "/games?live=all", "handball", HANDBALL_OFFSET));
        all.addAll(fetchGames(volleyballUrl + "/games?live=all", "volleyball", VOLLEYBALL_OFFSET));
        all.addAll(fetchGames(nflUrl + "/games?live=all", "nfl", NFL_OFFSET));
        return all;
    }

    public List<Match> fetchAllRecentFinishedMatches() {
        return fetchHistoricalMatches(1, 1);
    }

    public List<Match> fetchHistoricalMatches(int fromDaysAgo, int toDaysAgo) {
        if (!hasApiKey()) return Collections.emptyList();
        List<Match> all = new ArrayList<>();
        for (int daysAgo = fromDaysAgo; daysAgo <= toDaysAgo; daysAgo++) {
            String date = LocalDate.now().minusDays(daysAgo).toString();
            all.addAll(fetchFootball(footballUrl + "/fixtures?date=" + date, FOOTBALL_OFFSET));
            all.addAll(fetchGames(basketballUrl + "/games?date=" + date, "basketball", BASKETBALL_OFFSET));
            all.addAll(fetchGames(baseballUrl + "/games?date=" + date, "baseball", BASEBALL_OFFSET));
            all.addAll(fetchGames(hockeyUrl + "/games?date=" + date, "hockey", HOCKEY_OFFSET));
            all.addAll(fetchGames(rugbyUrl + "/games?date=" + date, "rugby", RUGBY_OFFSET));
            all.addAll(fetchGames(handballUrl + "/games?date=" + date, "handball", HANDBALL_OFFSET));
            all.addAll(fetchGames(volleyballUrl + "/games?date=" + date, "volleyball", VOLLEYBALL_OFFSET));
            all.addAll(fetchGames(nflUrl + "/games?date=" + date, "nfl", NFL_OFFSET));
        }
        return all;
    }

    private List<Match> fetchFootball(String url, long idOffset) {
        try {
            ApiFootballResponse response = restClient.get()
                    .uri(url)
                    .header("x-apisports-key", apiSportsKey)
                    .retrieve()
                    .body(ApiFootballResponse.class);
            if (response == null || response.getResponse() == null) return Collections.emptyList();
            return mapFootball(response, idOffset);
        } catch (RestClientException e) {
            log.error("Football fetch failed [{}]: {}", url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Match> fetchGames(String url, String sport, long idOffset) {
        try {
            ApiSportsGamesResponse response = restClient.get()
                    .uri(url)
                    .header("x-apisports-key", apiSportsKey)
                    .retrieve()
                    .body(ApiSportsGamesResponse.class);
            if (response == null || response.getResponse() == null) return Collections.emptyList();
            return mapGames(response, sport, idOffset);
        } catch (RestClientException e) {
            log.error("{} fetch failed [{}]: {}", sport, url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Match> mapFootball(ApiFootballResponse response, long idOffset) {
        return response.getResponse().stream()
                .map(fixture -> {
                    ApiFootballResponse.TeamsInfo teams = fixture.getTeams();
                    ApiFootballResponse.GoalsInfo goals = fixture.getGoals();
                    ApiFootballResponse.FixtureInfo fixtureInfo = fixture.getFixture();
                    ApiFootballResponse.LeagueInfo league = fixture.getLeague();

                    Team homeTeam = Team.builder()
                            .name(teams != null && teams.getHome() != null ? teams.getHome().getName() : null)
                            .sport("football")
                            .league(league != null ? league.getName() : null)
                            .build();

                    Team awayTeam = Team.builder()
                            .name(teams != null && teams.getAway() != null ? teams.getAway().getName() : null)
                            .sport("football")
                            .league(league != null ? league.getName() : null)
                            .build();

                    Long apiId = fixtureInfo != null ? fixtureInfo.getId() : null;
                    String rawStatus = fixtureInfo != null && fixtureInfo.getStatus() != null
                            ? fixtureInfo.getStatus().getShortStatus() : null;

                    return Match.builder()
                            .externalId(apiId != null ? apiId + idOffset : null)
                            .sport("football")
                            .date(fixtureInfo != null ? parseDate(fixtureInfo.getDate()) : null)
                            .homeTeam(homeTeam)
                            .awayTeam(awayTeam)
                            .homeScore(goals != null ? goals.getHome() : null)
                            .awayScore(goals != null ? goals.getAway() : null)
                            .status(normalizeStatus(rawStatus))
                            .build();
                })
                .toList();
    }

    private List<Match> mapGames(ApiSportsGamesResponse response, String sport, long idOffset) {
        return response.getResponse().stream()
                .map(entry -> {
                    ApiSportsGamesResponse.GameInfo gameInfo = entry.getGame();
                    ApiSportsGamesResponse.TeamsInfo teams = entry.getTeams();
                    ApiSportsGamesResponse.ScoresInfo scores = entry.getScores();
                    ApiSportsGamesResponse.LeagueInfo league = entry.getLeague();

                    Team homeTeam = Team.builder()
                            .name(teams != null && teams.getHome() != null ? teams.getHome().getName() : null)
                            .sport(sport)
                            .league(league != null ? league.getName() : null)
                            .build();

                    Team awayTeam = Team.builder()
                            .name(teams != null && teams.getAway() != null ? teams.getAway().getName() : null)
                            .sport(sport)
                            .league(league != null ? league.getName() : null)
                            .build();

                    Long apiId = gameInfo != null ? gameInfo.getId() : null;
                    String rawStatus = gameInfo != null && gameInfo.getStatus() != null
                            ? gameInfo.getStatus().getShortStatus() : null;

                    return Match.builder()
                            .externalId(apiId != null ? apiId + idOffset : null)
                            .sport(sport)
                            .date(gameInfo != null ? parseDate(gameInfo.getDate()) : null)
                            .homeTeam(homeTeam)
                            .awayTeam(awayTeam)
                            .homeScore(scores != null && scores.getHome() != null ? scores.getHome().getTotal() : null)
                            .awayScore(scores != null && scores.getAway() != null ? scores.getAway().getTotal() : null)
                            .status(normalizeStatus(rawStatus))
                            .build();
                })
                .toList();
    }

    private String normalizeStatus(String status) {
        if (status == null) return null;
        return FINISHED_STATUSES.contains(status) ? "FT" : status;
    }

    private boolean hasApiKey() {
        if (apiSportsKey == null || apiSportsKey.isBlank()) {
            log.warn("api.sports.key is not configured — skipping all fetches");
            return false;
        }
        return true;
    }

    private LocalDateTime parseDate(String isoDate) {
        if (isoDate == null) return null;
        try {
            return OffsetDateTime.parse(isoDate).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Could not parse date '{}': {}", isoDate, e.getMessage());
            return null;
        }
    }
}
