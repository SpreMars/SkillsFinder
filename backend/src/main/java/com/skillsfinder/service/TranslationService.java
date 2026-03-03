package com.skillsfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // LRU Cache with max size of 10000
    private final Map<String, String> translationCache = Collections.synchronizedMap(
        new LinkedHashMap<String, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 10000;
            }
        }
    );
    
    public String translateToChinese(String text) {
        return translateToChinese(text, null);
    }
    
    public String translateToChinese(String text, List<String> topics) {
        return translateToChinese(text, topics, false);
    }
    
    public String translateToChinese(String text, List<String> topics, boolean force) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        String trimmedText = text.trim();
        
        if (!force && translationCache.containsKey(trimmedText)) {
            String cachedTranslation = translationCache.get(trimmedText);
            return addCategoryLabel(cachedTranslation, topics);
        }
        
        try {
            String translatedText = translateWithMyMemory(trimmedText);
            
            if (translatedText != null && !translatedText.isEmpty()) {
                translationCache.put(trimmedText, translatedText);
                return addCategoryLabel(translatedText, topics);
            }
        } catch (Exception e) {
            log.warn("Translation failed: {}", e.getMessage());
        }
        
        return addCategoryLabel(trimmedText, topics);
    }
    
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MAX_RETRIES = 3;
    private static final double QUALITY_THRESHOLD = 0.2;
    
    private String translateWithMyMemory(String text) throws Exception {
        String trimmedText = text.length() > MAX_TEXT_LENGTH 
            ? text.substring(0, MAX_TEXT_LENGTH) 
            : text;
        
        String encodedText = URLEncoder.encode(trimmedText, StandardCharsets.UTF_8.toString());
        String url = "https://api.mymemory.translated.net/get?q=" + encodedText + "&langpair=en|zh-CN";
        
        if (url.length() > 2000) {
            log.warn("URL too long even after trimming, skipping translation");
            return text;
        }
        
        // Retry mechanism with exponential backoff
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String result = restTemplate.getForObject(url, String.class);
                JsonNode root = objectMapper.readTree(result);
                
                JsonNode responseData = root.path("responseData");
                String translatedText = responseData.path("translatedText").asText();
                
                double match = responseData.path("match").asDouble(0);
                
                // Check if translation failed or returned error message
                if (translatedText == null || translatedText.isEmpty() ||
                    translatedText.toUpperCase().contains("QUERY LENGTH LIMIT") ||
                    translatedText.toUpperCase().contains("ERROR") ||
                    translatedText.equals(text)) {
                    log.warn("Translation returned error or same text: '{}'", translatedText);
                    return text;
                }
                
                // Adjusted threshold from 0.3 to 0.2
                if (match < QUALITY_THRESHOLD) {
                    log.warn("Low quality translation (match={}): '{}' -> '{}'", match, text, translatedText);
                    return text;
                }
                
                // Log warning for translations with match between 0.2 and 0.3
                if (match < 0.3) {
                    log.warn("Acceptable but low quality translation (match={}): '{}' -> '{}'", match, text, translatedText);
                }
                
                return translatedText;
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Translation attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    // Exponential backoff: 1s, 2s, 4s
                    long delayMs = (long) Math.pow(2, attempt - 1) * 1000;
                    log.info("Retrying in {}ms...", delayMs);
                    Thread.sleep(delayMs);
                }
            }
        }
        
        // All retries failed
        log.error("All {} translation attempts failed for text: '{}'", MAX_RETRIES, text);
        throw lastException;
    }
    
    /**
     * Add intelligent category label based on project topics
     */
    private String addCategoryLabel(String translatedText, List<String> topics) {
        if (topics == null || topics.isEmpty() || translatedText == null) {
            return translatedText;
        }
        
        String label = determineCategoryLabel(topics);
        if (label != null) {
            return label + translatedText;
        }
        
        return translatedText;
    }
    
    /**
     * Determine category label based on topics
     */
    private String determineCategoryLabel(List<String> topics) {
        // Priority order: cursor-rules > gpt-prompts/system-prompt > ai-agent > claude
        
        for (String topic : topics) {
            if (topic.toLowerCase().contains("cursor-rules") || topic.toLowerCase().contains("cursor-rule")) {
                return "【Cursor 规则集】";
            }
        }
        
        for (String topic : topics) {
            if (topic.toLowerCase().contains("gpt-prompts") || topic.toLowerCase().contains("system-prompt")) {
                return "【AI 提示词工具】";
            }
        }
        
        for (String topic : topics) {
            if (topic.toLowerCase().contains("ai-agent")) {
                return "【AI 代理】";
            }
        }
        
        for (String topic : topics) {
            if (topic.toLowerCase().contains("claude")) {
                return "【Claude 工具】";
            }
        }
        
        return null;
    }
}
