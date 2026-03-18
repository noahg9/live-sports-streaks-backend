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

    // Runs once on startup, then at 00:00, 06:00, 12:00, 18:00 UTC
    @Scheduled(cron = "${scheduler.match-fetch.cron:0 0 0,6,12,18 * * *}")
    @Scheduled(initialDelay = 0, fixedDelay = Long.MAX_VALUE)
    public void fetchAndStoreMatches() {
        try {
            matchStoreService.fetchAndStore();
        } finally {
            streakService.calculateAndStoreTeamStreaks();
            streakService.calculateAndStorePlayerStreaks();
        }
    }
}
