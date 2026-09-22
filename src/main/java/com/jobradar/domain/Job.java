package com.jobradar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "job",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_job_source_source_url",
                        columnNames = {
                                "source",
                                "source_url"
                        }
                )
        }
)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String title;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate publishDate;

    @Column(
            nullable = false,
            length = 100
    )
    private String source;

    @Column(
            name = "source_url",
            nullable = false,
            length = 512
    )
    private String sourceUrl;

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

    protected Job() {
    }

    public Job(
            String company,
            String title,
            String location,
            String description,
            LocalDate publishDate,
            String source,
            String sourceUrl) {

        this.company = company;
        this.title = title;
        this.location = location;
        this.description = description;
        this.publishDate = publishDate;
        this.source = source;
        this.sourceUrl = sourceUrl;
    }

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt =
                LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public String getSource() {
        return source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {

        return "Job{"
                + "id=" + id
                + ", company='" + company + '\''
                + ", title='" + title + '\''
                + ", location='" + location + '\''
                + ", publishDate=" + publishDate
                + ", source='" + source + '\''
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}