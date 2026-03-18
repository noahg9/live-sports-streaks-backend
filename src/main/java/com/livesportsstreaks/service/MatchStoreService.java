package com.livesportsstreaks.service;

import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Team;
import com.livesportsstreaks.repository.MatchRepository;
import com.livesportsstreaks.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchStoreService {

    private static final Logger log = LoggerFactory.getLogger(MatchStoreService.class);

    private final MatchFetchService matchFetchService;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    public MatchStoreService(MatchFetchService matchFetchService,
                             TeamRepository teamRepository,
                             MatchRepository matchRepository) {
        this.matchFetchService = matchFetchService;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    public void fetchAndStore() {
        // HTTP calls happen here — outside any transaction
        List<Match> liveMatches = matchFetchService.fetchAllLiveMatches();
        List<Match> finishedMatches = matchFetchService.fetchAllRecentFinishedMatches();

        List<Match> allMatches = new ArrayList<>(liveMatches);
        allMatches.addAll(finishedMatches);

        if (allMatches.isEmpty()) {
            log.info("No matches to store");
            return;
        }
        storeMatches(allMatches);
        log.info("Processed {} matches across all sports ({} live, {} recently finished)",
                allMatches.size(), liveMatches.size(), finishedMatches.size());
    }

    public void fetchAndStoreHistorical(int pageSize) {
        // Find how far back our data already goes
        LocalDate earliestDate = matchRepository.findEarliestMatchDate()
                .map(dt -> dt.toLocalDate())
                .orElse(LocalDate.now()); // no data yet — start from today

        // Fetch the next page of history before our earliest date
        long daysAlreadyCovered = ChronoUnit.DAYS.between(earliestDate, LocalDate.now());
        int fetchFrom = (int) daysAlreadyCovered + 1;
        int fetchTo   = (int) daysAlreadyCovered + pageSize;

        log.info("Historical backfill: fetching days {} to {} back (earliest stored: {})",
                fetchFrom, fetchTo, earliestDate);

        List<Match> matches = matchFetchService.fetchHistoricalMatches(fetchFrom, fetchTo);
        if (matches.isEmpty()) {
            log.info("Historical backfill: no matches found for this date range");
            return;
        }
        storeMatches(matches);
        log.info("Historical backfill: processed {} matches (days {} to {} back)",
                matches.size(), fetchFrom, fetchTo);
    }

    private void storeMatches(List<Match> matches) {
        for (Match match : matches) {
            Team homeTeam = upsertTeam(match.getHomeTeam());
            Team awayTeam = upsertTeam(match.getAwayTeam());
            upsertMatch(match, homeTeam, awayTeam);
        }
    }

    private Team upsertTeam(Team team) {
        if (team == null || team.getName() == null) return null;
        return teamRepository.findByNameAndSport(team.getName(), team.getSport())
                .map(existing -> {
                    existing.setLeague(team.getLeague());
                    return teamRepository.save(existing);
                })
                .orElseGet(() -> teamRepository.save(team));
    }

    private void upsertMatch(Match match, Team homeTeam, Team awayTeam) {
        if (match.getExternalId() == null) {
            log.warn("Skipping match with null externalId");
            return;
        }

        Match existing = matchRepository.findByExternalId(match.getExternalId())
                .orElse(Match.builder().externalId(match.getExternalId()).build());

        existing.setSport(match.getSport());
        existing.setDate(match.getDate());
        existing.setHomeTeam(homeTeam);
        existing.setAwayTeam(awayTeam);
        existing.setHomeScore(match.getHomeScore());
        existing.setAwayScore(match.getAwayScore());
        existing.setStatus(match.getStatus());

        matchRepository.save(existing);
    }
}
