package com.livesportsstreaks.controller;

import com.livesportsstreaks.dto.StreakResponse;
import com.livesportsstreaks.service.StreakService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakControllerTest {

    @Mock
    private StreakService streakService;

    @InjectMocks
    private StreakController streakController;

    @Test
    void getAllStreaks_returnsList() {
        List<StreakResponse> expected = List.of(
                new StreakResponse("team", "Arsenal", "football", "win", 7)
        );
        when(streakService.getAllStreaks()).thenReturn(expected);

        List<StreakResponse> result = streakController.getAllStreaks();

        assertThat(result).isEqualTo(expected);
        verify(streakService).getAllStreaks();
    }

    @Test
    void getAllStreaks_returnsEmptyList() {
        when(streakService.getAllStreaks()).thenReturn(List.of());

        List<StreakResponse> result = streakController.getAllStreaks();

        assertThat(result).isEmpty();
    }

    @Test
    void getStreaksBySport_returnsList() {
        List<StreakResponse> expected = List.of(
                new StreakResponse("team", "Arsenal", "football", "win", 7)
        );
        when(streakService.getStreaksBySport("football")).thenReturn(expected);

        List<StreakResponse> result = streakController.getStreaksBySport("football");

        assertThat(result).isEqualTo(expected);
        verify(streakService).getStreaksBySport("football");
    }

    @Test
    void getStreaksBySport_returnsEmptyListWhenNoMatch() {
        when(streakService.getStreaksBySport("tennis")).thenReturn(List.of());

        List<StreakResponse> result = streakController.getStreaksBySport("tennis");

        assertThat(result).isEmpty();
    }

    @Test
    void getStreaksByTeam_returnsList() {
        List<StreakResponse> expected = List.of(
                new StreakResponse("team", "Arsenal", "football", "win", 7)
        );
        when(streakService.getStreaksByTeam(1L)).thenReturn(expected);

        List<StreakResponse> result = streakController.getStreaksByTeam(1L);

        assertThat(result).isEqualTo(expected);
        verify(streakService).getStreaksByTeam(1L);
    }

    @Test
    void getStreaksByTeam_returnsEmptyListWhenNoMatch() {
        when(streakService.getStreaksByTeam(99L)).thenReturn(List.of());

        List<StreakResponse> result = streakController.getStreaksByTeam(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getStreaksByPlayer_returnsList() {
        List<StreakResponse> expected = List.of(
                new StreakResponse("player", "LeBron James", "basketball", "win", 5)
        );
        when(streakService.getStreaksByPlayer(1L)).thenReturn(expected);

        List<StreakResponse> result = streakController.getStreaksByPlayer(1L);

        assertThat(result).isEqualTo(expected);
        verify(streakService).getStreaksByPlayer(1L);
    }

    @Test
    void getStreaksByPlayer_returnsEmptyListWhenNoMatch() {
        when(streakService.getStreaksByPlayer(99L)).thenReturn(List.of());

        List<StreakResponse> result = streakController.getStreaksByPlayer(99L);

        assertThat(result).isEmpty();
    }
}
