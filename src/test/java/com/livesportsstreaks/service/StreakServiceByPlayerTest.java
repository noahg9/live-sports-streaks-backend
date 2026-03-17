package com.livesportsstreaks.service;

import com.livesportsstreaks.dto.StreakResponse;
import com.livesportsstreaks.model.Player;
import com.livesportsstreaks.model.Streak;
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
class StreakServiceByPlayerTest {

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
    void getStreaksByPlayer_returnsOnlyStreaksForGivenPlayer() {
        Player lebron = Player.builder().id(1L).name("LeBron James").sport("basketball").build();
        Streak lebronWin = Streak.builder()
                .entityType("player").entityId(1L).streakType("win").length(5).build();
        Streak lebronUnbeaten = Streak.builder()
                .entityType("player").entityId(1L).streakType("unbeaten").length(8).build();
        Streak otherPlayerStreak = Streak.builder()
                .entityType("player").entityId(2L).streakType("win").length(3).build();
        when(streakRepository.findAll()).thenReturn(List.of(lebronWin, lebronUnbeaten, otherPlayerStreak));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(lebron));

        List<StreakResponse> result = streakService.getStreaksByPlayer(1L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> "LeBron James".equals(r.name()));
        assertThat(result).allMatch(r -> "basketball".equals(r.sport()));
        assertThat(result).allMatch(r -> "player".equals(r.entityType()));
        assertThat(result).extracting(StreakResponse::streakType)
                .containsExactlyInAnyOrder("win", "unbeaten");
    }

    @Test
    void getStreaksByPlayer_returnsEmptyWhenNoMatch() {
        Streak lebronWin = Streak.builder()
                .entityType("player").entityId(1L).streakType("win").length(5).build();
        when(streakRepository.findAll()).thenReturn(List.of(lebronWin));

        List<StreakResponse> result = streakService.getStreaksByPlayer(99L);

        assertThat(result).isEmpty();
    }
}
