package dev.lin.exquis.blockedStory;

import dev.lin.exquis.story.StoryEntity;
import dev.lin.exquis.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_stories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedStoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private StoryEntity story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by", nullable = false)
    private UserEntity lockedBy;

    @Column(name = "blocked_until", nullable = false)
    private LocalDateTime blockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
