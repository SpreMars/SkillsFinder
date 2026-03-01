package com.skillsfinder.repository;

import com.skillsfinder.entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    
    Optional<Skill> findByFullName(String fullName);
    
    Page<Skill> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<Skill> findByLanguage(String language, Pageable pageable);
    
    @Query("SELECT s FROM Skill s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR s.name LIKE %:keyword% OR s.description LIKE %:keyword%) " +
           "AND (:language IS NULL OR :language = '' OR s.language = :language) " +
           "ORDER BY s.starCount DESC")
    Page<Skill> searchSkills(@Param("keyword") String keyword,
                            @Param("language") String language,
                            Pageable pageable);
    
    @Query("SELECT DISTINCT s.language FROM Skill s WHERE s.language IS NOT NULL ORDER BY s.language")
    List<String> findAllLanguages();
    
    @Query("SELECT s.fullName FROM Skill s")
    List<String> findAllFullNames();
    
    boolean existsByFullName(String fullName);
}
