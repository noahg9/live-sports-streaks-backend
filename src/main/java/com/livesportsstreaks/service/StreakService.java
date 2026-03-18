package com.livesportsstreaks.service;

import com.livesportsstreaks.dto.StreakResponse;
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

import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    // Sports where draws are possible — unbeaten streak is meaningful
    private static final Set<String> DRAW_SPORTS = Set.of("football", "rugby", "handball");

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
            upsertStreak("team", team.getId(), "win", calculateWinStreak(matches, team.getId()));
            if (DRAW_SPORTS.contains(team.getSport())) {
                upsertStreak("team", team.getId(), "unbeaten", calculateUnbeatenStreak(matches, team.getId()));
            }
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
            upsertStreak("player", player.getId(), "win", calculateWinStreak(matches, teamId));
            if (DRAW_SPORTS.contains(player.getSport())) {
                upsertStreak("player", player.getId(), "unbeaten", calculateUnbeatenStreak(matches, teamId));
            }
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

    @Transactional(readOnly = true)
    public List<StreakResponse> getAllStreaks() {
        return streakRepository.findAll().stream()
                .map(this::toStreakResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StreakResponse> getStreaksBySport(String sport) {
        return streakRepository.findAll().stream()
                .map(this::toStreakResponse)
                .filter(r -> sport.equals(r.sport()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StreakResponse> getStreaksByTeam(Long teamId) {
        return streakRepository.findAll().stream()
                .filter(s -> "team".equals(s.getEntityType()) && teamId.equals(s.getEntityId()))
                .map(this::toStreakResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StreakResponse> getStreaksByPlayer(Long playerId) {
        return streakRepository.findAll().stream()
                .filter(s -> "player".equals(s.getEntityType()) && playerId.equals(s.getEntityId()))
                .map(this::toStreakResponse)
                .collect(Collectors.toList());
    }

    private StreakResponse toStreakResponse(Streak streak) {
        String name;
        String sport;
        String league;
        if ("team".equals(streak.getEntityType())) {
            Team team = teamRepository.findById(streak.getEntityId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Team not found for streak: entityId=" + streak.getEntityId()));
            name = team.getName();
            sport = team.getSport();
            league = team.getLeague();
        } else {
            Player player = playerRepository.findById(streak.getEntityId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Player not found for streak: entityId=" + streak.getEntityId()));
            name = player.getName();
            sport = player.getSport();
            league = player.getTeam() != null ? player.getTeam().getLeague() : null;
        }
        return new StreakResponse(streak.getEntityType(), name, sport, league,
                streak.getStreakType(), streak.getLength(), streak.getLastUpdated());
    }
}
