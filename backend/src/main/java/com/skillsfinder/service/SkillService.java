package com.skillsfinder.service;

import com.skillsfinder.entity.Skill;
import com.skillsfinder.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {
    
    private final SkillRepository skillRepository;
    
    public Page<Skill> getAllSkills(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("starCount").descending());
        return skillRepository.findAll(pageable);
    }
    
    public Page<Skill> searchSkills(String keyword, String language, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return skillRepository.searchSkills(keyword, language, pageable);
    }
    
    public Skill getSkillById(Long id) {
        return skillRepository.findById(id).orElse(null);
    }
    
    public Skill getSkillByFullName(String fullName) {
        return skillRepository.findByFullName(fullName).orElse(null);
    }
    
    public List<String> getAllLanguages() {
        return skillRepository.findAllLanguages();
    }
    
    public List<String> getAllFullNames() {
        return skillRepository.findAllFullNames();
    }
    
    @Transactional
    public Skill saveSkill(Skill skill) {
        if (skill.getLastUpdated() == null) {
            skill.setLastUpdated(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        skill.setLastCrawledAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        Skill existingSkill = skillRepository.findByFullName(skill.getFullName()).orElse(null);
        if (existingSkill != null) {
            skill.setPreviousStarCount(existingSkill.getStarCount());
            skill.setStarChange(skill.getStarCount() - existingSkill.getStarCount());
            skill.setId(existingSkill.getId());
        } else {
            skill.setPreviousStarCount(skill.getStarCount());
            skill.setStarChange(0);
        }
        
        return skillRepository.save(skill);
    }
    
    @Transactional
    public void deleteSkill(Long id) {
        skillRepository.deleteById(id);
    }
    
    public boolean existsByFullName(String fullName) {
        return skillRepository.existsByFullName(fullName);
    }
    
    public List<Skill> getTrendingSkills(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("starCount").descending());
        Page<Skill> page = skillRepository.findAll(pageable);
        return page.getContent();
    }
    
    public List<Skill> getAllSkillsForTranslation() {
        return skillRepository.findAll();
    }
    
    public List<Skill> getTopByStars(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("starCount").descending());
        return skillRepository.findAll(pageable).getContent();
    }
    
    public List<Skill> getTopByStarGrowth(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("starChange").descending());
        return skillRepository.findAll(pageable).getContent();
    }
    
    public List<Skill> getNewRepos(int days, int limit) {
        List<Skill> allSkills = skillRepository.findAll();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        String cutoffStr = cutoff.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        return allSkills.stream()
            .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().compareTo(cutoffStr) >= 0)
            .sorted((a, b) -> b.getStarCount().compareTo(a.getStarCount()))
            .limit(limit)
            .toList();
    }
    
    public List<Skill> getTopByLanguage(String language, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("starCount").descending());
        return skillRepository.findByLanguage(language, pageable).getContent();
    }
    
    public List<String> getPopularLanguages() {
        List<Skill> allSkills = skillRepository.findAll();
        return allSkills.stream()
            .filter(s -> s.getLanguage() != null && !s.getLanguage().isEmpty())
            .collect(java.util.stream.Collectors.groupingBy(Skill::getLanguage, java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(java.util.Map.Entry::getKey)
            .toList();
    }
}
