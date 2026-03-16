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
class StreakServiceGetAllTest {

    @Mock private TeamRepository teamRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private StreakRepository streakRepository;
    @Mock private PlayerRepository playerRepository;

    @InjectMocks
    private StreakService streakService;

    @Test
    void getAllStreaks_enrichesTeamStreak() {
        Team team = Team.builder().id(1L).name("Arsenal").sport("football").build();
        Streak streak = Streak.builder()
                .entityType("team").entityId(1L).streakType("win").length(7).build();
        when(streakRepository.findAll()).thenReturn(List.of(streak));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        List<StreakResponse> result = streakService.getAllStreaks();

        assertThat(result).hasSize(1);
        StreakResponse r = result.get(0);
        assertThat(r.entityType()).isEqualTo("team");
        assertThat(r.name()).isEqualTo("Arsenal");
        assertThat(r.sport()).isEqualTo("football");
        assertThat(r.streakType()).isEqualTo("win");
        assertThat(r.length()).isEqualTo(7);
    }

    @Test
    void getAllStreaks_enrichesPlayerStreak() {
        Player player = Player.builder().id(2L).name("LeBron James").sport("basketball").build();
        Streak streak = Streak.builder()
                .entityType("player").entityId(2L).streakType("win").length(5).build();
        when(streakRepository.findAll()).thenReturn(List.of(streak));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player));

        List<StreakResponse> result = streakService.getAllStreaks();

        assertThat(result).hasSize(1);
        StreakResponse r = result.get(0);
        assertThat(r.entityType()).isEqualTo("player");
        assertThat(r.name()).isEqualTo("LeBron James");
        assertThat(r.sport()).isEqualTo("basketball");
        assertThat(r.streakType()).isEqualTo("win");
        assertThat(r.length()).isEqualTo(5);
    }
}
