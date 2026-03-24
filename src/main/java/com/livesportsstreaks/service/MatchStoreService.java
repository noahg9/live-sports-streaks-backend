package com.livesportsstreaks.service;

import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Team;
import com.livesportsstreaks.repository.MatchRepository;
import com.livesportsstreaks.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

public void storeMatches(List<Match> matches) {
        int skipped = 0;
        for (Match match : matches) {
            if (match.getExternalId() == null) { skipped++; continue; }
            Team homeTeam = upsertTeam(match.getHomeTeam());
            Team awayTeam = upsertTeam(match.getAwayTeam());
            upsertMatch(match, homeTeam, awayTeam);
        }
        if (skipped > 0) log.warn("Skipped {} match(es) with null externalId", skipped);
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
