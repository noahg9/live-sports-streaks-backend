package com.livesportsstreaks.service;

import com.livesportsstreaks.dto.ApiFootballResponse;
import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class MatchFetchService {

    private static final Logger log = LoggerFactory.getLogger(MatchFetchService.class);

    private final RestClient restClient;

    @Value("${api.football.url}")
    private String apiFootballUrl;

    @Value("${api.sports.key}")
    private String apiSportsKey;

    public MatchFetchService() {
        this.restClient = RestClient.builder().build();
    }

    public List<Match> fetchLiveFootballMatches() {
        if (apiSportsKey == null || apiSportsKey.isBlank()) {
            log.warn("api.sports.key is not configured — skipping football fetch");
            return Collections.emptyList();
        }

        try {
            ApiFootballResponse response = restClient.get()
                    .uri(apiFootballUrl + "/fixtures?live=all")
                    .header("x-apisports-key", apiSportsKey)
                    .retrieve()
                    .body(ApiFootballResponse.class);

            if (response == null || response.getResponse() == null) {
                log.warn("API-Football returned null or empty response");
                return Collections.emptyList();
            }

            return mapToMatches(response);
        } catch (RestClientException e) {
            log.error("Failed to fetch live football matches: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Match> mapToMatches(ApiFootballResponse response) {
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

                    LocalDateTime matchDate = fixtureInfo != null ? parseDate(fixtureInfo.getDate()) : null;
                    String status = fixtureInfo != null && fixtureInfo.getStatus() != null
                            ? fixtureInfo.getStatus().getShortStatus()
                            : null;

                    return Match.builder()
                            .sport("football")
                            .date(matchDate)
                            .homeTeam(homeTeam)
                            .awayTeam(awayTeam)
                            .homeScore(goals != null ? goals.getHome() : null)
                            .awayScore(goals != null ? goals.getAway() : null)
                            .status(status)
                            .build();
                })
                .toList();
    }

    private LocalDateTime parseDate(String isoDate) {
        if (isoDate == null) return null;
        try {
            return OffsetDateTime.parse(isoDate).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Could not parse match date '{}': {}", isoDate, e.getMessage());
            return null;
        }
    }
}
