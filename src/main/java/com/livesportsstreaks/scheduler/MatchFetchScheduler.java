package com.livesportsstreaks.scheduler;

import com.livesportsstreaks.service.MatchStoreService;
import org.springframework.stereotype.Component;

@Component
public class MatchFetchScheduler {

    private final MatchStoreService matchStoreService;

    public MatchFetchScheduler(MatchStoreService matchStoreService) {
        this.matchStoreService = matchStoreService;
    }

    // @Scheduled added in story 2-3
    public void fetchAndStoreMatches() {
        matchStoreService.fetchAndStore();
    }
}
