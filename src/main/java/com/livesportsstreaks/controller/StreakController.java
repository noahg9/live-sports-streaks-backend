package com.livesportsstreaks.controller;

import com.livesportsstreaks.dto.StreakResponse;
import com.livesportsstreaks.service.StreakService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StreakController {

    private final StreakService streakService;

    public StreakController(StreakService streakService) {
        this.streakService = streakService;
    }

    @GetMapping("/streaks")
    public List<StreakResponse> getAllStreaks() {
        return streakService.getAllStreaks();
    }

    @GetMapping("/streaks/{sport}")
    public List<StreakResponse> getStreaksBySport(@PathVariable String sport) {
        return streakService.getStreaksBySport(sport);
    }
}
