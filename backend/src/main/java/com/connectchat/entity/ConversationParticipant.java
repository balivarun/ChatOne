package com.connectchat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "conversation_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"conversation", "user", "lastReadMessage"})
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "is_muted")
    @Builder.Default
    private Boolean isMuted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private Message lastReadMessage;

    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = Instant.now();
        if (this.isArchived == null) this.isArchived = false;
        if (this.isPinned == null) this.isPinned = false;
        if (this.isMuted == null) this.isMuted = false;
    }
}
