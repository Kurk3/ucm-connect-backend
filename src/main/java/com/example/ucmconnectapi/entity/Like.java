package com.example.ucmconnectapi.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Like entity - represents likes on posts or comments
 * A user can like either a post OR a comment, but not both
 */
@Entity
@Table(name = "likes",
    indexes = {
        @Index(name = "idx_likes_user_post", columnList = "user_id, post_id"),
        @Index(name = "idx_likes_user_comment", columnList = "user_id, comment_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_likes_user_post", columnNames = {"user_id", "post_id"}),
        @UniqueConstraint(name = "uk_likes_user_comment", columnNames = {"user_id", "comment_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_likes_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", foreignKey = @ForeignKey(name = "fk_likes_post"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", foreignKey = @ForeignKey(name = "fk_likes_comment"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Like() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Validates that a like is associated with either a post OR a comment, but not both
     */
    @PrePersist
    @PreUpdate
    private void validateLikeTarget() {
        boolean hasPost = post != null;
        boolean hasComment = comment != null;

        if (!hasPost && !hasComment) {
            throw new IllegalStateException("Like must be associated with either a post or a comment");
        }

        if (hasPost && hasComment) {
            throw new IllegalStateException("Like cannot be associated with both a post and a comment");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Like)) return false;
        Like like = (Like) o;
        return id != null && id.equals(like.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
