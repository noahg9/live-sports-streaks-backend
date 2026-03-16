package com.livesportsstreaks.repository;

import com.livesportsstreaks.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByNameAndSport(String name, String sport);
}
