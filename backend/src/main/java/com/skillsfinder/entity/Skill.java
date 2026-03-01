package com.skillsfinder.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "skills")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Skill implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "full_name")
    private String fullName;
    
    private String owner;
    
    private String repository;
    
    private String language;
    
    @Column(name = "star_count")
    private Integer starCount;
    
    @Column(name = "fork_count")
    private Integer forkCount;
    
    @Column(name = "html_url")
    private String htmlUrl;
    
    @Column(name = "readme_content", columnDefinition = "TEXT")
    private String readmeContent;
    
    @Column(name = "topics", columnDefinition = "TEXT")
    private String topics;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "last_updated")
    private String lastUpdated;
    
    @Column(name = "created_at")
    private String createdAt;
    
    @Column(name = "previous_star_count")
    private Integer previousStarCount;
    
    @Column(name = "star_change")
    private Integer starChange;
    
    @Column(name = "last_crawled_at")
    private String lastCrawledAt;
}
