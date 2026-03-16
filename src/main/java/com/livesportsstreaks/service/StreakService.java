package com.livesportsstreaks.service;

import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Player;
import com.livesportsstreaks.model.Streak;
import com.livesportsstreaks.model.Team;
import com.livesportsstreaks.repository.MatchRepository;
import com.livesportsstreaks.repository.PlayerRepository;
import com.livesportsstreaks.repository.StreakRepository;
import com.livesportsstreaks.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final StreakRepository streakRepository;
    private final PlayerRepository playerRepository;

    public StreakService(TeamRepository teamRepository,
                        MatchRepository matchRepository,
                        StreakRepository streakRepository,
                        PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.streakRepository = streakRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public void calculateAndStoreTeamStreaks() {
        List<Team> teams = teamRepository.findAll();
        for (Team team : teams) {
            List<Match> matches = matchRepository.findFinishedMatchesByTeamOrderByDateDesc(team.getId());
            int winStreak = calculateWinStreak(matches, team.getId());
            int unbeatenStreak = calculateUnbeatenStreak(matches, team.getId());
            upsertStreak("team", team.getId(), "win", winStreak);
            upsertStreak("team", team.getId(), "unbeaten", unbeatenStreak);
        }
        log.info("Calculated streaks for {} teams", teams.size());
    }

    @Transactional
    public void calculateAndStorePlayerStreaks() {
        List<Player> players = playerRepository.findAll();
        Map<Long, List<Match>> teamMatchCache = new HashMap<>();
        int skipped = 0;
        for (Player player : players) {
            if (player.getTeam() == null) {
                skipped++;
                continue;
            }
            Long teamId = player.getTeam().getId();
            List<Match> matches = teamMatchCache.computeIfAbsent(teamId,
                    id -> matchRepository.findFinishedMatchesByTeamOrderByDateDesc(id));
            int winStreak = calculateWinStreak(matches, teamId);
            int unbeatenStreak = calculateUnbeatenStreak(matches, teamId);
            upsertStreak("player", player.getId(), "win", winStreak);
            upsertStreak("player", player.getId(), "unbeaten", unbeatenStreak);
        }
        log.info("Calculated streaks for {} players ({} skipped — no team)", players.size() - skipped, skipped);
    }

    private int calculateWinStreak(List<Match> matches, Long teamId) {
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

    private int calculateUnbeatenStreak(List<Match> matches, Long teamId) {
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

    private void upsertStreak(String entityType, Long entityId, String streakType, int length) {
        Streak streak = streakRepository
                .findByEntityTypeAndEntityIdAndStreakType(entityType, entityId, streakType)
                .orElse(Streak.builder()
                        .entityType(entityType)
                        .entityId(entityId)
                        .streakType(streakType)
                        .build());
        streak.setLength(length);
        streak.setLastUpdated(LocalDateTime.now());
        streakRepository.save(streak);
    }
}
