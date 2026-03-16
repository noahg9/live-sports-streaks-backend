package com.livesportsstreaks.service;

import com.livesportsstreaks.dto.StreakResponse;
import com.livesportsstreaks.model.Streak;
import com.livesportsstreaks.model.Team;
import com.livesportsstreaks.repository.MatchRepository;
import com.livesportsstreaks.repository.PlayerRepository;
import com.livesportsstreaks.repository.StreakRepository;
import com.livesportsstreaks.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceByTeamTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private StreakRepository streakRepository;
    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private StreakService streakService;

    @Test
    void getStreaksByTeam_returnsOnlyStreaksForGivenTeam() {
        Team arsenal = Team.builder().id(1L).name("Arsenal").sport("football").build();
        Streak arsenalWin = Streak.builder()
                .entityType("team").entityId(1L).streakType("win").length(7).build();
        Streak arsenalUnbeaten = Streak.builder()
                .entityType("team").entityId(1L).streakType("unbeaten").length(10).build();
        Streak lakersStreak = Streak.builder()
                .entityType("team").entityId(2L).streakType("win").length(5).build();
        when(streakRepository.findAll()).thenReturn(List.of(arsenalWin, arsenalUnbeaten, lakersStreak));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(arsenal));

        List<StreakResponse> result = streakService.getStreaksByTeam(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "Arsenal".equals(r.name()));
        assertThat(result).allMatch(r -> "football".equals(r.sport()));
    }

    @Test
    void getStreaksByTeam_returnsEmptyWhenNoMatch() {
        Streak arsenalWin = Streak.builder()
                .entityType("team").entityId(1L).streakType("win").length(7).build();
        when(streakRepository.findAll()).thenReturn(List.of(arsenalWin));

        List<StreakResponse> result = streakService.getStreaksByTeam(99L);

        assertThat(result).isEmpty();
    }
}
