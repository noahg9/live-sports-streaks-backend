package com.livesportsstreaks.service;

import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Player;
import com.livesportsstreaks.model.Streak;
import com.livesportsstreaks.model.Team;
import com.livesportsstreaks.repository.MatchRepository;
import com.livesportsstreaks.repository.PlayerRepository;
import com.livesportsstreaks.repository.StreakRepository;
import com.livesportsstreaks.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServicePlayerTest {

    @Mock private TeamRepository teamRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private StreakRepository streakRepository;
    @Mock private PlayerRepository playerRepository;

    @InjectMocks
    private StreakService streakService;

    private Team teamA;
    private Player playerA;

    @BeforeEach
    void setUp() {
        teamA = Team.builder().id(1L).name("Arsenal").sport("football").build();
        playerA = Player.builder().id(10L).name("Salah").sport("football").team(teamA).build();
        lenient().when(streakRepository.findByEntityTypeAndEntityIdAndStreakType(any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Match finishedMatch(Long homeTeamId, Integer homeScore, Long awayTeamId, Integer awayScore) {
        Team home = Team.builder().id(homeTeamId).build();
        Team away = Team.builder().id(awayTeamId).build();
        return Match.builder()
                .homeTeam(home).awayTeam(away)
                .homeScore(homeScore).awayScore(awayScore)
                .status("FT").build();
    }

    private List<Streak> captureAllSavedStreaks() {
        streakService.calculateAndStorePlayerStreaks();
        ArgumentCaptor<Streak> captor = ArgumentCaptor.forClass(Streak.class);
        verify(streakRepository, times(2)).save(captor.capture());
        return captor.getAllValues();
    }

    private Streak streakOf(List<Streak> streaks, String type) {
        return streaks.stream()
                .filter(s -> type.equals(s.getStreakType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No streak of type: " + type));
    }

    // ── Win streak tests ──────────────────────────────────────────────────────

    @Test
    void threeConsecutiveTeamWins_playerWinStreakIs3() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 3, 2L, 0),
                finishedMatch(1L, 2, 2L, 1),
                finishedMatch(1L, 1, 2L, 0)
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(3);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(3);
        assertThat(streakOf(saved, "win").getEntityType()).isEqualTo("player");
        assertThat(streakOf(saved, "win").getEntityId()).isEqualTo(10L);
    }

    @Test
    void awayWin_countsTowardPlayerStreak() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        // teamA is the away team (id=1L), wins 0-2
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(2L, 0, 1L, 2)
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(1);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(1);
    }

    @Test
    void drawBreaksPlayerWinStreakButNotUnbeatenStreak() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 2, 2L, 0),  // win (most recent)
                finishedMatch(1L, 1, 2L, 1)   // draw — breaks win, not unbeaten
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(1);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(2);
    }

    @Test
    void lossAsFirstResult_bothPlayerStreaksZero() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 0, 2L, 1),  // loss (most recent) — breaks both streaks
                finishedMatch(1L, 3, 2L, 0)   // prior win — not counted
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(0);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(0);
    }

    @Test
    void nullScores_treatedAsLoss_forPlayer() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, null, 2L, null),  // null scores — treated as non-win
                finishedMatch(1L, 2, 2L, 0)         // prior win — not counted
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(0);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(0);
    }

    // ── Null team guard ───────────────────────────────────────────────────────

    @Test
    void playerWithNullTeam_isSkipped() {
        Player playerNoTeam = Player.builder().id(99L).name("Free Agent").sport("football").team(null).build();
        when(playerRepository.findAll()).thenReturn(List.of(playerNoTeam));

        streakService.calculateAndStorePlayerStreaks();

        verify(streakRepository, never()).save(any());
        verify(matchRepository, never()).findFinishedMatchesByTeamOrderByDateDesc(any());
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void noFinishedMatchesForTeam_bothStreaksZero() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of());

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(0);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(0);
    }

    @Test
    void twoPlayersOnSameTeam_eachGetsOwnStreakRows() {
        Player playerB = Player.builder().id(20L).name("Firmino").sport("football").team(teamA).build();
        when(playerRepository.findAll()).thenReturn(List.of(playerA, playerB));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 2, 2L, 0)
        ));

        streakService.calculateAndStorePlayerStreaks();

        ArgumentCaptor<Streak> captor = ArgumentCaptor.forClass(Streak.class);
        verify(streakRepository, times(4)).save(captor.capture()); // 2 players × 2 streak types

        List<Streak> playerAStreaks = captor.getAllValues().stream()
                .filter(s -> s.getEntityId().equals(10L)).toList();
        List<Streak> playerBStreaks = captor.getAllValues().stream()
                .filter(s -> s.getEntityId().equals(20L)).toList();

        assertThat(playerAStreaks).hasSize(2);
        assertThat(playerBStreaks).hasSize(2);
    }

    @Test
    void twoPlayersOnSameTeam_matchQueryCalledOnce() {
        // Cache: second player on the same team reuses the cached match list — only 1 DB query
        Player playerB = Player.builder().id(20L).name("Firmino").sport("football").team(teamA).build();
        when(playerRepository.findAll()).thenReturn(List.of(playerA, playerB));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 2, 2L, 0)
        ));

        streakService.calculateAndStorePlayerStreaks();

        verify(matchRepository, times(1)).findFinishedMatchesByTeamOrderByDateDesc(1L);
    }

    // ── Upsert tests ─────────────────────────────────────────────────────────

    @Test
    void existingPlayerStreak_isUpdatedNotDuplicated() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 1, 2L, 0)
        ));
        Streak existingWin = Streak.builder()
                .id(55L).entityType("player").entityId(10L).streakType("win").length(7).build();
        when(streakRepository.findByEntityTypeAndEntityIdAndStreakType("player", 10L, "win"))
                .thenReturn(Optional.of(existingWin));

        streakService.calculateAndStorePlayerStreaks();

        assertThat(existingWin.getLength()).isEqualTo(1);
        assertThat(existingWin.getId()).isEqualTo(55L); // same DB row, not a new insert
        verify(streakRepository).save(existingWin);
    }

    @Test
    void savedPlayerStreak_hasLastUpdatedSet() {
        when(playerRepository.findAll()).thenReturn(List.of(playerA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of());

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(saved).allMatch(s -> s.getLastUpdated() != null);
    }
}
