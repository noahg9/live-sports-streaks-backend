package com.livesportsstreaks.repository;

import com.livesportsstreaks.model.Streak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StreakRepository extends JpaRepository<Streak, Long> {
    Optional<Streak> findByEntityTypeAndEntityIdAndStreakType(
            String entityType, Long entityId, String streakType);
}
