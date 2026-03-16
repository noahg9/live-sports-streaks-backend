package com.livesportsstreaks.service;

import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Streak;
import com.livesportsstreaks.model.Team;
import com.livesportsstreaks.repository.MatchRepository;
import com.livesportsstreaks.repository.StreakRepository;
import com.livesportsstreaks.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final StreakRepository streakRepository;

    public StreakService(TeamRepository teamRepository,
                        MatchRepository matchRepository,
                        StreakRepository streakRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.streakRepository = streakRepository;
    }

    @Transactional
    public void calculateAndStoreTeamStreaks() {
        List<Team> teams = teamRepository.findAll();
        for (Team team : teams) {
            int winStreak = calculateWinStreak(team.getId());
            int unbeatenStreak = calculateUnbeatenStreak(team.getId());
            upsertTeamStreak(team.getId(), "win", winStreak);
            upsertTeamStreak(team.getId(), "unbeaten", unbeatenStreak);
        }
        log.info("Calculated streaks for {} teams", teams.size());
    }

    private int calculateWinStreak(Long teamId) {
        List<Match> matches = matchRepository.findFinishedMatchesByTeamOrderByDateDesc(teamId);
        int streak = 0;
        for (Match match : matches) {
            if (teamWon(match, teamId)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateUnbeatenStreak(Long teamId) {
        List<Match> matches = matchRepository.findFinishedMatchesByTeamOrderByDateDesc(teamId);
        int streak = 0;
        for (Match match : matches) {
            if (teamWonOrDrew(match, teamId)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private boolean teamWon(Match match, Long teamId) {
        if (match.getHomeScore() == null || match.getAwayScore() == null) return false;
        if (teamId.equals(match.getHomeTeam().getId())) {
            return match.getHomeScore() > match.getAwayScore();
        } else {
            return match.getAwayScore() > match.getHomeScore();
        }
    }

    private boolean teamWonOrDrew(Match match, Long teamId) {
        if (match.getHomeScore() == null || match.getAwayScore() == null) return false;
        if (teamId.equals(match.getHomeTeam().getId())) {
            return match.getHomeScore() >= match.getAwayScore();
        } else {
            return match.getAwayScore() >= match.getHomeScore();
        }
    }

    private void upsertTeamStreak(Long teamId, String streakType, int length) {
        Streak streak = streakRepository
                .findByEntityTypeAndEntityIdAndStreakType("team", teamId, streakType)
                .orElse(Streak.builder()
                        .entityType("team")
                        .entityId(teamId)
                        .streakType(streakType)
                        .build());
        streak.setLength(length);
        streak.setLastUpdated(LocalDateTime.now());
        streakRepository.save(streak);
    }
}
