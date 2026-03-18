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
        List<Match> liveMatches = matchFetchService.fetchLiveFootballMatches();
        List<Match> finishedMatches = matchFetchService.fetchRecentFinishedFootballMatches();

        List<Match> allMatches = new ArrayList<>();
        allMatches.addAll(liveMatches);
        allMatches.addAll(finishedMatches);

        if (allMatches.isEmpty()) {
            log.info("No football matches to store");
            return;
        }
        storeMatches(allMatches);
        log.info("Processed {} football matches ({} live, {} recently finished)",
                allMatches.size(), liveMatches.size(), finishedMatches.size());
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
