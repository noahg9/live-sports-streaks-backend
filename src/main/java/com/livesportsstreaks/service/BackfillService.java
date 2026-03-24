package com.livesportsstreaks.service;

import com.livesportsstreaks.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BackfillService {

    private static final Logger log = LoggerFactory.getLogger(BackfillService.class);

    // 2100ms between each sport request keeps us safely under 30 req/min
    private static final long DELAY_MS = 2100;

    private final MatchFetchService matchFetchService;
    private final MatchStoreService matchStoreService;
    private final StreakService streakService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public BackfillService(MatchFetchService matchFetchService,
                           MatchStoreService matchStoreService,
                           StreakService streakService) {
        this.matchFetchService = matchFetchService;
        this.matchStoreService = matchStoreService;
        this.streakService = streakService;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Kicks off a background backfill for the given number of past days. Returns false if one is already running. */
    public boolean start(int days) {
        if (!running.compareAndSet(false, true)) return false;
        Thread thread = new Thread(() -> {
            try {
                run(days);
            } finally {
                running.set(false);
            }
        }, "backfill-thread");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void run(int days) {
        Set<String> sports = matchFetchService.getSupportedSports();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days);

        // Per-sport tracking (going backwards — most recent first):
        //   activeTeams:    teams seen so far that have NOT lost yet
        //   eliminatedTeams: teams that have had at least one loss
        // Once activeTeams is empty (and we've seen at least one match), that sport needs
        // no further history — every team's current streak is already fully captured.
        Map<String, Set<String>> activeTeams = new HashMap<>();
        Map<String, Set<String>> eliminatedTeams = new HashMap<>();
        Set<String> completedSports = new HashSet<>();

        for (String sport : sports) {
            activeTeams.put(sport, new HashSet<>());
            eliminatedTeams.put(sport, new HashSet<>());
        }

        log.info("Backfill started: {} days ({} → {}), {} sports", days, today, startDate, sports.size());

        long daysProcessed = 0;

        for (LocalDate date = today; !date.isBefore(startDate); date = date.minusDays(1)) {
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Backfill interrupted at {}", date);
                break;
            }
            if (completedSports.size() == sports.size()) {
                log.info("All sports complete — stopping backfill early at {}", date);
                break;
            }

            for (String sport : sports) {
                if (completedSports.contains(sport)) continue;

                var matches = matchFetchService.fetchForSport(date.toString(), sport);
                matchStoreService.storeMatches(matches);
                updateTracking(sport, matches, activeTeams.get(sport), eliminatedTeams.get(sport));

                if (isSportComplete(sport, activeTeams.get(sport), eliminatedTeams.get(sport))) {
                    completedSports.add(sport);
                    log.info("Sport '{}' complete at {} — all {} known teams have had a loss, no need to go further back",
                            sport, date, eliminatedTeams.get(sport).size());
                }

                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            daysProcessed++;
            if (daysProcessed % 10 == 0) {
                log.info("Backfill progress: {} days back, {}/{} sports still active",
                        daysProcessed, sports.size() - completedSports.size(), sports.size());
            }
        }

        log.info("Backfill fetching done ({} days processed) — recalculating streaks", daysProcessed);
        streakService.calculateAndStoreTeamStreaks();
        streakService.calculateAndStorePlayerStreaks();
        log.info("Backfill complete");
    }

    /**
     * Updates per-sport active/eliminated sets based on the finished matches for one day.
     * Going backwards, a team is eliminated once we see a loss — their current streak
     * cannot extend past that date, so no earlier data is needed for them.
     * Draws are not losses, so they don't eliminate a team (conservative: ensures unbeaten
     * streaks are fully captured in draw-capable sports like football).
     */
    private void updateTracking(String sport,
                                 java.util.List<Match> matches,
                                 Set<String> active,
                                 Set<String> eliminated) {
        for (Match match : matches) {
            if (!"FT".equals(match.getStatus())) continue;
            if (match.getHomeScore() == null || match.getAwayScore() == null) continue;
            if (match.getHomeTeam() == null || match.getAwayTeam() == null) continue;

            String home = match.getHomeTeam().getName();
            String away = match.getAwayTeam().getName();
            if (home == null || away == null) continue;

            boolean homeWon = match.getHomeScore() > match.getAwayScore();
            boolean awayWon = match.getAwayScore() > match.getHomeScore();

            processTeam(home, homeWon, awayWon, active, eliminated); // home wins when homeWon, loses when awayWon
            processTeam(away, awayWon, homeWon, active, eliminated); // away wins when awayWon, loses when homeWon
        }
    }

    private void processTeam(String team, boolean won, boolean lost,
                              Set<String> active, Set<String> eliminated) {
        if (eliminated.contains(team)) return; // already know they lost — ignore all earlier results
        if (lost) {
            eliminated.add(team);
            active.remove(team);
        } else {
            // won or drew — still potentially on a streak
            active.add(team);
        }
    }

    private boolean isSportComplete(String sport, Set<String> active, Set<String> eliminated) {
        // Complete when: we've seen at least one team AND all seen teams have had a loss
        return !eliminated.isEmpty() && active.isEmpty();
    }
}
