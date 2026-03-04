package com.skillsfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsfinder.entity.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubCrawlerService {
    
    private final SkillService skillService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    @Value("${github.api.token:}")
    private String githubToken;
    
    @Value("${github.api.search-url:https://api.github.com/search/repositories}")
    private String searchUrl;
    
    private static final String[] SEARCH_QUERIES = {
        "topic:claude-agent-system-prompt",
        "topic:cursor-rules", 
        "topic:ai-agent",
        "topic:gpt-prompts",
        "topic:system-prompt",
        "claude code workflow",
        "cursor rule"
    };
    
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000)
    public void scheduledCrawl() {
        log.info("Starting scheduled GitHub crawl (every 2 hours)...");
        crawlSkills();
    }
    
    public void crawlSkills() {
        log.info("Starting GitHub crawl with translation and updates...");
        
        List<String> existingFullNames = skillService.getAllFullNames();
        List<String> existingFullNamesList = new ArrayList<>(existingFullNames);
        
        AtomicInteger newCount = new AtomicInteger(0);
        AtomicInteger updateCount = new AtomicInteger(0);
        
        for (String query : SEARCH_QUERIES) {
            try {
                crawlByQuery(query, existingFullNamesList, newCount, updateCount);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("Error crawling query: {}", query, e);
            }
        }
        
        log.info("GitHub crawl completed. New: {}, Updated: {}", newCount.get(), updateCount.get());
    }
    
    private void crawlByQuery(String query, List<String> existingFullNames, AtomicInteger newCount, AtomicInteger updateCount) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String url = searchUrl + "?q=" + encodedQuery + "&sort=stars&order=desc&per_page=30";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            if (githubToken != null && !githubToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + githubToken);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("items");
                
                for (JsonNode item : items) {
                    Skill skill = parseSkill(item);
                    String fullName = skill.getFullName();
                    
                    if (!existingFullNames.contains(fullName)) {
                        skillService.saveSkill(skill);
                        existingFullNames.add(fullName);
                        newCount.incrementAndGet();
                    } else {
                        Skill existing = skillService.getSkillByFullName(fullName);
                        if (existing != null) {
                            boolean updated = false;
                            if (!skill.getStarCount().equals(existing.getStarCount())) {
                                existing.setStarCount(skill.getStarCount());
                                updated = true;
                            }
                            if (!skill.getForkCount().equals(existing.getForkCount())) {
                                existing.setForkCount(skill.getForkCount());
                                updated = true;
                            }
                            if (skill.getDescription() != null && !skill.getDescription().equals(existing.getDescription())) {
                                existing.setDescription(skill.getDescription());
                                updated = true;
                            }
                            if (updated) {
                                skillService.saveSkill(existing);
                                updateCount.incrementAndGet();
                            }
                        }
                    }
                }
                
                log.info("Processed {} items for query: {}", items.size(), query);
            }
        } catch (Exception e) {
            log.error("Failed to crawl query: {}", query, e);
        }
    }
    
    private Skill parseSkill(JsonNode item) {
        Skill skill = new Skill();
        skill.setName(item.path("name").asText());
        skill.setFullName(item.path("full_name").asText());
        
        // Parse topics from JSON array
        JsonNode topicsNode = item.path("topics");
        List<String> topicsList = new ArrayList<>();
        if (topicsNode.isArray()) {
            for (JsonNode topicNode : topicsNode) {
                topicsList.add(topicNode.asText());
            }
        }
        
        skill.setDescription(item.path("description").asText());
        
        skill.setOwner(item.path("owner").path("login").asText());
        skill.setRepository(item.path("name").asText());
        skill.setLanguage(item.path("language").asText(null));
        skill.setStarCount(item.path("stargazers_count").asInt(0));
        skill.setForkCount(item.path("forks_count").asInt(0));
        skill.setHtmlUrl(item.path("html_url").asText());
        skill.setAvatarUrl(item.path("owner").path("avatar_url").asText());
        
        String topics = item.path("topics").toString();
        skill.setTopics(topics);
        
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        skill.setCreatedAt(now);
        skill.setLastUpdated(now);
        
        return skill;
    }
}
