package com.jobradar.domain;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import jakarta.persistence.Column;
@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String company;
    private String title;
    private String location;
    @Column(columnDefinition = "TEXT")
    private String description;
    private LocalDate publishDate;
    private String source;
    private String sourceUrl;
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

    @Override
    public String toString() {
        return "Job{"
                + "company=" + company
                + ", title=" + title
                + ", location=" + location
                + ", publishDate=" + publishDate
                + ", source=" + source
                + "}";

    }
}
