package com.livesportsstreaks.repository;

import com.livesportsstreaks.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByExternalId(Long externalId);

    @Query("SELECT MIN(m.date) FROM Match m WHERE m.date IS NOT NULL")
    Optional<LocalDateTime> findEarliestMatchDate();

    @Query("SELECT m FROM Match m " +
           "JOIN FETCH m.homeTeam " +
           "JOIN FETCH m.awayTeam " +
           "WHERE (m.homeTeam.id = :teamId OR m.awayTeam.id = :teamId) " +
           "AND m.status = 'FT' " +
           "ORDER BY m.date DESC")
    List<Match> findFinishedMatchesByTeamOrderByDateDesc(@Param("teamId") Long teamId);
}
