package com.livesportsstreaks.scheduler;

import com.livesportsstreaks.service.MatchStoreService;
import com.livesportsstreaks.service.StreakService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchFetchScheduler {

    private final MatchStoreService matchStoreService;
    private final StreakService streakService;

    @Value("${scheduler.historical-fetch.days-back:7}")
    private int historicalDaysBack;

    public MatchFetchScheduler(MatchStoreService matchStoreService, StreakService streakService) {
        this.matchStoreService = matchStoreService;
        this.streakService = streakService;
    }

    // Every 6 hours: fetch live + yesterday for all sports
    @Scheduled(
            fixedDelayString = "${scheduler.match-fetch.fixed-delay:21600000}",
            initialDelayString = "${scheduler.match-fetch.initial-delay:0}")
    public void fetchAndStoreMatches() {
        try {
            matchStoreService.fetchAndStore();
        } finally {
            streakService.calculateAndStoreTeamStreaks();
            streakService.calculateAndStorePlayerStreaks();
        }
    }

    // Once daily at 3am UTC: fetch last N days for all sports to build up historical data
    @Scheduled(cron = "${scheduler.historical-fetch.cron:0 0 3 * * *}")
    public void fetchAndStoreHistorical() {
        try {
            matchStoreService.fetchAndStoreHistorical(historicalDaysBack);
        } finally {
            streakService.calculateAndStoreTeamStreaks();
            streakService.calculateAndStorePlayerStreaks();
        }
    }
}
