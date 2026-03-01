// package com.skillsfinder;
//
// import com.skillsfinder.entity.Skill;
// import com.skillsfinder.repository.SkillRepository;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;
//
// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
//
// @Component
// public class DataInitializer implements CommandLineRunner {
//     
//     private final SkillRepository skillRepository;
//     
//     public DataInitializer(SkillRepository skillRepository) {
//         this.skillRepository = skillRepository;
//     }
//     
//     @Override
//     public void run(String... args) {
//         if (skillRepository.count() == 0) {
//             List<Skill> sampleSkills = Arrays.asList(
//                 createSkill("Cursor Rules", "ppin巷/cursor-rules-cn", "为 Cursor IDE 打造的中文编程规则和提示词", "TypeScript", 1250, 89, "cursor-rules"),
//                 createSkill("Claude Code Workflows", "anthroping/claude-code", "官方 Claude Code 工作流和示例", "Python", 3200, 456, "claude-code"),
//                 createSkill("AI Agent Prompts", "shopify/world-cursor-ai", "Shopify 的 AI Agent 提示词集合", "JavaScript", 2100, 234, "ai-agent"),
//                 createSkill("System Prompt Collection", "langgptai/awesome-system-prompts", "精选的系统提示词集合", null, 890, 67, "system-prompt"),
//                 createSkill("GPT Prompts", "f/awesome-chatgpt-prompts", "ChatGPT 提示词大全", "Python", 8500, 1200, "gpt-prompts"),
//                 createSkill("Devin Workflows", "cognition-labs/devin", "AI 程序员 Devin 工作流", "Python", 5600, 789, "ai-agent"),
//                 createSkill("Cursor Rules Collection", "mrdgptai/cursor-rules", "各种编程语言的 Cursor 规则", "JavaScript", 680, 45, "cursor-rules"),
//                 createSkill("AI Coder Prompts", "anthropic/anthropic-sdk-python", "Anthropic Python SDK 示例", "Python", 4200, 567, "claude-code"),
//                 createSkill("Vercel AI SDK", "vercel/ai", "Vercel AI 开发套件", "TypeScript", 9800, 1100, "ai-agent"),
//                 createSkill("LangChain Examples", "langchain-ai/langchain", "LangChain 官方示例", "Python", 15000, 2300, "ai-agent")
//             );
//             
//             skillRepository.saveAll(sampleSkills);
//             System.out.println("Sample data initialized: " + sampleSkills.size() + " skills");
//         }
//     }
//     
//     private Skill createSkill(String name, String fullName, String description, 
//                              String language, int stars, int forks, String topic) {
//         Skill skill = new Skill();
//         skill.setName(name);
//         skill.setFullName(fullName);
//         skill.setDescription(description);
//         skill.setOwner(fullName.split("/")[0]);
//         skill.setRepository(fullName.split("/")[1]);
//         skill.setLanguage(language);
//         skill.setStarCount(stars);
//         skill.setForkCount(forks);
//         skill.setHtmlUrl("https://github.com/" + fullName);
//         skill.setAvatarUrl("https://avatars.githubusercontent.com/u/xxxxx?v=4");
//         skill.setTopics("[\"" + topic + "\"]");
//         skill.setCreatedAt(LocalDateTime.now());
//         skill.setLastUpdated(LocalDateTime.now());
//         return skill;
//     }
// }
