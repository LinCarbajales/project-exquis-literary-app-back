package dev.lin.exquis.collaboration;

import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collaborations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 260)
    private String text;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relaciones
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private StoryEntity story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
}
