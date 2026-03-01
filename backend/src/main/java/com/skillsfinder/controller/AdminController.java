package com.skillsfinder.controller;

import com.skillsfinder.entity.Skill;
import com.skillsfinder.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final SkillService skillService;
    
    @PostMapping("/add-skill")
    public ResponseEntity<?> addSkill(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received payload: {}", payload);
            
            Skill skill = new Skill();
            skill.setName((String) payload.get("name"));
            skill.setFullName((String) payload.get("fullName"));
            skill.setDescription((String) payload.get("description"));
            skill.setOwner((String) payload.get("owner"));
            skill.setRepository((String) payload.get("repository"));
            skill.setLanguage((String) payload.get("language"));
            
            if (payload.get("starCount") != null) {
                skill.setStarCount(Integer.valueOf(payload.get("starCount").toString()));
            }
            if (payload.get("forkCount") != null) {
                skill.setForkCount(Integer.valueOf(payload.get("forkCount").toString()));
            }
            
            skill.setHtmlUrl((String) payload.get("htmlUrl"));
            skill.setAvatarUrl((String) payload.get("avatarUrl"));
            skill.setTopics((String) payload.get("topics"));
            
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            skill.setCreatedAt(now);
            skill.setLastUpdated(now);
            
            skill = skillService.saveSkill(skill);
            
            return ResponseEntity.ok("Skill added: " + skill.getName());
        } catch (Exception e) {
            log.error("Error adding skill", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
