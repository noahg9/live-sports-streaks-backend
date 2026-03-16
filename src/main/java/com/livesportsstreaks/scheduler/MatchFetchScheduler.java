package com.livesportsstreaks.scheduler;

import com.livesportsstreaks.service.MatchStoreService;
import com.livesportsstreaks.service.StreakService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchFetchScheduler {

    private final MatchStoreService matchStoreService;
    private final StreakService streakService;

    public MatchFetchScheduler(MatchStoreService matchStoreService, StreakService streakService) {
        this.matchStoreService = matchStoreService;
        this.streakService = streakService;
    }

    @Scheduled(
            fixedDelayString = "${scheduler.match-fetch.fixed-delay:3600000}",
            initialDelayString = "${scheduler.match-fetch.initial-delay:0}")
    public void fetchAndStoreMatches() {
        try {
            matchStoreService.fetchAndStore();
        } finally {
            streakService.calculateAndStoreTeamStreaks();
            streakService.calculateAndStorePlayerStreaks();
        }
    }
}
