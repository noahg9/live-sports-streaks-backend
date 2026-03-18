package com.livesportsstreaks.controller;

import com.livesportsstreaks.service.MatchStoreService;
import com.livesportsstreaks.service.StreakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final MatchStoreService matchStoreService;
    private final StreakService streakService;

    @Value("${scheduler.historical-fetch.days-back:60}")
    private int historicalDaysBack;

    public AdminController(MatchStoreService matchStoreService, StreakService streakService) {
        this.matchStoreService = matchStoreService;
        this.streakService = streakService;
    }

    @PostMapping("/backfill")
    public String triggerHistoricalBackfill() {
        log.info("Manual historical backfill triggered");
        matchStoreService.fetchAndStoreHistorical(historicalDaysBack);
        streakService.calculateAndStoreTeamStreaks();
        return "Historical backfill complete";
    }

    @PostMapping("/refresh")
    public String triggerRefresh() {
        log.info("Manual refresh triggered");
        matchStoreService.fetchAndStore();
        streakService.calculateAndStoreTeamStreaks();
        return "Refresh complete";
    }
}
