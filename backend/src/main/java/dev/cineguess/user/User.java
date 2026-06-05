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
    private String email;
    private String passwordHash;
    private Instant createdAt;

    public User() {
    }

    public User(UUID id, String username, Instant createdAt) {
        this(id, username, username, null, createdAt);
    }

    public User(UUID id, String username, String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.newEntity = true;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
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

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
