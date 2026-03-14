package com.livesportsstreaks.repository;

import com.livesportsstreaks.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
