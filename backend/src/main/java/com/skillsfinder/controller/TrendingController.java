package com.skillsfinder.controller;

import com.skillsfinder.entity.Skill;
import com.skillsfinder.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class TrendingController {
    
    private final SkillService skillService;
    
    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrending() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("total", skillService.getTopByStars(20));
        result.put("growth", skillService.getTopByStarGrowth(20));
        result.put("new", skillService.getNewRepos(7, 20));
        
        List<String> popularLanguages = skillService.getPopularLanguages();
        Map<String, List<Skill>> languageMap = new HashMap<>();
        for (String lang : popularLanguages) {
            languageMap.put(lang, skillService.getTopByLanguage(lang, 10));
        }
        result.put("language", languageMap);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/trending/total")
    public ResponseEntity<List<Skill>> getTotalTrending(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(skillService.getTopByStars(limit));
    }
    
    @GetMapping("/trending/growth")
    public ResponseEntity<List<Skill>> getGrowthTrending(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(skillService.getTopByStarGrowth(limit));
    }
    
    @GetMapping("/trending/new")
    public ResponseEntity<List<Skill>> getNewTrending(@RequestParam(defaultValue = "7") int days,
                                                      @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(skillService.getNewRepos(days, limit));
    }
    
    @GetMapping("/trending/language/{language}")
    public ResponseEntity<List<Skill>> getLanguageTrending(@PathVariable String language,
                                                           @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(skillService.getTopByLanguage(language, limit));
    }
    
    @GetMapping("/trending/languages")
    public ResponseEntity<List<String>> getPopularLanguages() {
        return ResponseEntity.ok(skillService.getPopularLanguages());
    }
}
