package com.livesportsstreaks.repository;

import com.livesportsstreaks.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
