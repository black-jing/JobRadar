package com.jobradar.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "target_direction",
            nullable = false,
            length = 200
    )
    private String targetDirection;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_profile_skill",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    @Column(
            name = "skill",
            nullable = false,
            length = 100
    )
    private List<String> skills =
            new ArrayList<>();

    @Column(
            name = "experience_summary",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String experienceSummary;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    protected UserProfile() {
    }

    public UserProfile(
            String targetDirection,
            List<String> skills,
            String experienceSummary) {

        update(
                targetDirection,
                skills,
                experienceSummary
        );
    }

    public void update(
            String targetDirection,
            List<String> skills,
            String experienceSummary) {

        this.targetDirection =
                targetDirection;

        this.skills =
                new ArrayList<>(skills);

        this.experienceSummary =
                experienceSummary;
    }

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTargetDirection() {
        return targetDirection;
    }

    public List<String> getSkills() {
        return skills;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}