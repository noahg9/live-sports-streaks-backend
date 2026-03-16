package com.livesportsstreaks.scheduler;

import com.livesportsstreaks.service.MatchStoreService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchFetchScheduler {

    private final MatchStoreService matchStoreService;

    public MatchFetchScheduler(MatchStoreService matchStoreService) {
        this.matchStoreService = matchStoreService;
    }

    @Scheduled(
            fixedDelayString = "${scheduler.match-fetch.fixed-delay:3600000}",
            initialDelayString = "${scheduler.match-fetch.initial-delay:0}")
    public void fetchAndStoreMatches() {
        matchStoreService.fetchAndStore();
    }
}
