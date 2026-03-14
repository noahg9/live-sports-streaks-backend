package com.livesportsstreaks.repository;

import com.livesportsstreaks.model.Streak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreakRepository extends JpaRepository<Streak, Long> {
}
