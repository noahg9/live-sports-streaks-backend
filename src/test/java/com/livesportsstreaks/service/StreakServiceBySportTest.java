package com.livesportsstreaks.service;

import com.livesportsstreaks.dto.StreakResponse;
import com.livesportsstreaks.model.Player;
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
class StreakServiceBySportTest {

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
    void getStreaksBySport_returnsOnlyMatchingSport() {
        Team arsenal = Team.builder().id(1L).name("Arsenal").sport("football").build();
        Team lakers = Team.builder().id(2L).name("Lakers").sport("basketball").build();
        Streak footballStreak = Streak.builder()
                .entityType("team").entityId(1L).streakType("win").length(7).build();
        Streak basketballStreak = Streak.builder()
                .entityType("team").entityId(2L).streakType("win").length(5).build();
        when(streakRepository.findAll()).thenReturn(List.of(footballStreak, basketballStreak));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(arsenal));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(lakers));

        List<StreakResponse> result = streakService.getStreaksBySport("football");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Arsenal");
        assertThat(result.get(0).sport()).isEqualTo("football");
    }

    @Test
    void getStreaksBySport_returnsEmptyWhenNoMatch() {
        Team arsenal = Team.builder().id(1L).name("Arsenal").sport("football").build();
        Streak footballStreak = Streak.builder()
                .entityType("team").entityId(1L).streakType("win").length(7).build();
        when(streakRepository.findAll()).thenReturn(List.of(footballStreak));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(arsenal));

        List<StreakResponse> result = streakService.getStreaksBySport("tennis");

        assertThat(result).isEmpty();
    }
}
