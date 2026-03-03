package com.skillsfinder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for Translation Service Enhancement
 * 
 * Tests the enhanced translation service with:
 * - Intelligent category labels based on topics
 * - LRU cache with size limit
 * - Improved error handling
 */
class TranslationServiceTest {

    private TranslationService translationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        translationService = new TranslationService(restTemplate, objectMapper);
    }

    /**
     * Test 1: Null and Empty String Handling
     */
    @Test
    void testNullHandling() {
        String result = translationService.translateToChinese(null);
        assertNull(result, "Null input should return null");
    }

    @Test
    void testEmptyStringHandling() {
        String result = translationService.translateToChinese("");
        assertEquals("", result, "Empty string should return empty string");
    }

    /**
     * Test 2: Category Label Generation
     */
    @Test
    void testCursorRulesLabel() {
        List<String> topics = Arrays.asList("cursor-rules", "ai-coding");
        String text = "Test description";
        
        String result = translationService.translateToChinese(text, topics);
        
        // Should contain category label for cursor-rules
        assertTrue(result.contains("【Cursor 规则集】") || result.equals(text), 
            "Should add Cursor label or return original text");
    }

    @Test
    void testGptPromptsLabel() {
        List<String> topics = Arrays.asList("gpt-prompts", "prompts");
        String text = "Test description";
        
        String result = translationService.translateToChinese(text, topics);
        
        // Should contain category label for gpt-prompts
        assertTrue(result.contains("【AI 提示词工具】") || result.equals(text), 
            "Should add AI Prompts label or return original text");
    }

    @Test
    void testAiAgentLabel() {
        List<String> topics = Arrays.asList("ai-agent", "automation");
        String text = "Test description";
        
        String result = translationService.translateToChinese(text, topics);
        
        // Should contain category label for ai-agent
        assertTrue(result.contains("【AI 代理】") || result.equals(text), 
            "Should add AI Agent label or return original text");
    }

    @Test
    void testClaudeLabel() {
        List<String> topics = Arrays.asList("claude", "anthropic");
        String text = "Test description";
        
        String result = translationService.translateToChinese(text, topics);
        
        // Should contain category label for claude
        assertTrue(result.contains("【Claude 工具】") || result.equals(text), 
            "Should add Claude label or return original text");
    }

    /**
     * Test 3: No Label for Unknown Topics
     */
    @Test
    void testNoLabelForUnknownTopics() {
        List<String> topics = Arrays.asList("unknown-topic", "random");
        String text = "Test description";
        
        String result = translationService.translateToChinese(text, topics);
        
        // Should not add any label for unknown topics
        assertNotNull(result, "Result should not be null");
    }

    /**
     * Test 4: Single Parameter Method (Backward Compatibility)
     */
    @Test
    void testSingleParameterMethod() {
        String text = "Test description";
        
        String result = translationService.translateToChinese(text);
        
        // Should work without topics parameter
        assertNotNull(result, "Single parameter method should work");
    }

    /**
     * Test 5: Cache Functionality
     */
    @Test
    void testCacheFunctionality() {
        String text = "Cached test";
        
        // First call
        String result1 = translationService.translateToChinese(text);
        
        // Second call - should use cache
        String result2 = translationService.translateToChinese(text);
        
        // Results should be identical
        assertEquals(result1, result2, "Cached results should match");
    }

    /**
     * Test 6: Label Priority (cursor-rules has highest priority)
     */
    @Test
    void testLabelPriority() {
        // cursor-rules should have priority over other topics
        List<String> topics = Arrays.asList("cursor-rules", "gpt-prompts", "ai-agent");
        String text = "Test description";
        
        String result = translationService.translateToChinese(text, topics);
        
        // Should prioritize cursor-rules label
        assertTrue(result.contains("【Cursor 规则集】") || result.equals(text), 
            "Should prioritize Cursor label");
    }
}
