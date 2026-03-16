package com.livesportsstreaks.service;

import com.livesportsstreaks.model.Match;
import com.livesportsstreaks.model.Streak;
import com.livesportsstreaks.model.Team;
import com.livesportsstreaks.repository.MatchRepository;
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
class StreakServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private StreakRepository streakRepository;

    @InjectMocks
    private StreakService streakService;

    private Team teamA;

    @BeforeEach
    void setUp() {
        teamA = Team.builder().id(1L).name("Arsenal").sport("football").build();
        when(streakRepository.findByEntityTypeAndEntityIdAndStreakType(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Match finishedMatch(Long homeTeamId, Integer homeScore, Long awayTeamId, Integer awayScore) {
        Team home = Team.builder().id(homeTeamId).build();
        Team away = Team.builder().id(awayTeamId).build();
        return Match.builder()
                .homeTeam(home)
                .awayTeam(away)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .status("FT")
                .build();
    }

    private List<Streak> captureAllSavedStreaks() {
        streakService.calculateAndStoreTeamStreaks();
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
    void threeConsecutiveHomeWins_winStreakIs3() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 3, 2L, 0),
                finishedMatch(1L, 2, 2L, 1),
                finishedMatch(1L, 1, 2L, 0)
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(3);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(3);
    }

    @Test
    void awayWin_countsTowardStreak() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        // teamA is away team (id=1L), wins 0-2
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(2L, 0, 1L, 2)
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(1);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(1);
    }

    @Test
    void drawBreaksWinStreakButNotUnbeatenStreak() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        // Most recent: win, then draw — draw breaks win streak but not unbeaten
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 2, 2L, 0),  // win (most recent)
                finishedMatch(1L, 1, 2L, 1)   // draw — breaks win streak, not unbeaten
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(1);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(2);
    }

    @Test
    void lossAsFirstResult_bothStreaksZero() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        // teamA is home, loses 0-1 (most recent match)
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 0, 2L, 1),  // loss (most recent) — breaks both streaks immediately
                finishedMatch(1L, 3, 2L, 0)   // prior win — not counted
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(0);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(0);
    }

    @Test
    void nullScores_treatedAsLoss() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, null, 2L, null),  // null scores — treated as non-win/non-unbeaten
                finishedMatch(1L, 2, 2L, 0)         // prior win — not counted
        ));

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(0);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(0);
    }

    @Test
    void noFinishedMatches_bothStreaksZero() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of());

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(streakOf(saved, "win").getLength()).isEqualTo(0);
        assertThat(streakOf(saved, "unbeaten").getLength()).isEqualTo(0);
    }

    // ── Upsert tests ─────────────────────────────────────────────────────────

    @Test
    void existingStreak_isUpdatedNotDuplicated() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 1, 2L, 0)
        ));
        Streak existingWin = Streak.builder()
                .id(10L).entityType("team").entityId(1L).streakType("win").length(5).build();
        when(streakRepository.findByEntityTypeAndEntityIdAndStreakType("team", 1L, "win"))
                .thenReturn(Optional.of(existingWin));

        streakService.calculateAndStoreTeamStreaks();

        // Existing object should have been mutated (length updated) and re-saved — no new row
        assertThat(existingWin.getLength()).isEqualTo(1);
        assertThat(existingWin.getId()).isEqualTo(10L);
        verify(streakRepository).save(existingWin);
    }

    @Test
    void savedStreak_hasLastUpdatedSet() {
        when(teamRepository.findAll()).thenReturn(List.of(teamA));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of());

        List<Streak> saved = captureAllSavedStreaks();

        assertThat(saved).allMatch(s -> s.getLastUpdated() != null);
    }

    @Test
    void multipleTeams_eachGetsOwnStreaks() {
        Team teamB = Team.builder().id(2L).name("Chelsea").sport("football").build();
        when(teamRepository.findAll()).thenReturn(List.of(teamA, teamB));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(1L)).thenReturn(List.of(
                finishedMatch(1L, 2, 2L, 0)  // teamA wins
        ));
        when(matchRepository.findFinishedMatchesByTeamOrderByDateDesc(2L)).thenReturn(List.of(
                finishedMatch(1L, 2, 2L, 0)  // teamB (away) loses
        ));

        streakService.calculateAndStoreTeamStreaks();

        ArgumentCaptor<Streak> captor = ArgumentCaptor.forClass(Streak.class);
        verify(streakRepository, times(4)).save(captor.capture()); // 2 teams × 2 streak types

        List<Streak> teamAStreaks = captor.getAllValues().stream()
                .filter(s -> s.getEntityId().equals(1L)).toList();
        List<Streak> teamBStreaks = captor.getAllValues().stream()
                .filter(s -> s.getEntityId().equals(2L)).toList();

        assertThat(teamAStreaks).hasSize(2);
        assertThat(teamBStreaks).hasSize(2);
    }
}
