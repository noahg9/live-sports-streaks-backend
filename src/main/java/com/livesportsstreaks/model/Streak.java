package com.livesportsstreaks.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "streaks", uniqueConstraints = @UniqueConstraint(columnNames = {"entity_type", "entity_id", "streak_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType;  // "team" or "player"
    private Long entityId;
    private String streakType;  // "win" or "unbeaten"
    private Integer length;
    private LocalDateTime lastUpdated;
}
