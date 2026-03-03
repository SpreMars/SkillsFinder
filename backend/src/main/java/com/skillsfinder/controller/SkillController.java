package com.skillsfinder.controller;

import com.skillsfinder.entity.Skill;
import com.skillsfinder.service.GitHubCrawlerService;
import com.skillsfinder.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SkillController {
    
    private final SkillService skillService;
    private final GitHubCrawlerService githubCrawlerService;
    
    @GetMapping
    public ResponseEntity<?> getSkills(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        try {
            Page<Skill> skillsPage;
            
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            boolean hasLanguage = language != null && !language.trim().isEmpty();
            
            if (hasKeyword || hasLanguage) {
                skillsPage = skillService.searchSkills(keyword, language, page, size);
            } else {
                skillsPage = skillService.getAllSkills(page, size);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", skillsPage.getContent());
            response.put("totalPages", skillsPage.getTotalPages());
            response.put("totalElements", skillsPage.getTotalElements());
            response.put("currentPage", skillsPage.getNumber());
            response.put("pageSize", skillsPage.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting skills", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getSkillById(@PathVariable Long id) {
        try {
            Skill skill = skillService.getSkillById(id);
            if (skill != null) {
                return ResponseEntity.ok(skill);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting skill by id", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/languages")
    public ResponseEntity<List<String>> getAllLanguages() {
        return ResponseEntity.ok(skillService.getAllLanguages());
    }
    
    @PostMapping("/crawl")
    public ResponseEntity<Map<String, String>> triggerCrawl() {
        new Thread(() -> githubCrawlerService.crawlSkills()).start();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Crawl started");
        return ResponseEntity.ok(response);
    }
}
