package com.livesportsstreaks.controller;

import com.livesportsstreaks.service.BackfillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class BackfillController {

    private final BackfillService backfillService;

    public BackfillController(BackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @PostMapping("/backfill")
    public ResponseEntity<String> startBackfill(@RequestParam(defaultValue = "365") int days) {
        if (days < 1 || days > 1825) {
            return ResponseEntity.badRequest().body("days must be between 1 and 1825");
        }
        boolean started = backfillService.start(days);
        if (!started) {
            return ResponseEntity.status(409).body("Backfill already in progress");
        }
        long maxMinutes = (long) days * 8 * 2100 / 60_000;
        return ResponseEntity.accepted()
                .body("Backfill started for up to " + days + " days (max ~" + maxMinutes
                        + " min — stops early per sport once all teams have had a loss). "
                        + "Check /admin/backfill/status for progress.");
    }

    @GetMapping("/backfill/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok(backfillService.isRunning() ? "running" : "idle");
    }
}
