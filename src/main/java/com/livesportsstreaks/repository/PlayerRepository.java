package com.livesportsstreaks.repository;

import com.livesportsstreaks.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
}
