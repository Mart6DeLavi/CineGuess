package dev.cineguess.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
public class User implements Persistable<UUID> {

    @Id
    private UUID id;
    @Transient
    private boolean newEntity;
    private String username;
    private Instant createdAt;

    public User() {
    }

    public User(UUID id, String username, Instant createdAt) {
        this.id = id;
        this.newEntity = true;
        this.username = username;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
