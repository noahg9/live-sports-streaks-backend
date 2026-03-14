package com.livesportsstreaks.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "streaks")
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
