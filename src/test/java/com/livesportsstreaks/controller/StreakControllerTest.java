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
}
