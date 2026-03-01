# Translation Enhancement Bugfix Design

## Overview

本设计文档描述了对后端翻译服务的优化方案。当前 TranslationService 在处理 GitHub 项目简介翻译时存在四个主要问题：缺乏重试机制导致临时故障时直接返回英文、缺少智能分类标签影响用户体验、翻译质量阈值过于严格、缓存机制缺少大小限制。本次修复将在保持零预算约束（继续使用免费 MyMemory API）的前提下，通过添加重试逻辑、实现基于 topics 的智能分类、调整质量阈值、引入 LRU 缓存来解决这些问题。

## Glossary

- **Bug_Condition (C)**: 触发缺陷的条件 - MyMemory API 调用失败、翻译结果缺少分类标签、低质量翻译被丢弃、缓存无限增长
- **Property (P)**: 期望的正确行为 - API 失败时自动重试、翻译结果包含智能分类标签、使用合理的质量阈值、使用有界 LRU 缓存
- **Preservation**: 必须保持不变的现有行为 - 空值处理、文本长度限制、缓存命中逻辑、只翻译 description 字段、MyMemory API 使用
- **TranslationService**: `backend/src/main/java/com/skillsfinder/service/TranslationService.java` 中的服务类，负责调用 MyMemory API 进行英译中
- **GitHubCrawlerService**: `backend/src/main/java/com/skillsfinder/service/GitHubCrawlerService.java` 中的服务类，负责爬取 GitHub 项目数据并调用翻译服务
- **topics**: GitHub 项目的主题标签数组，存储在 Skill 实体的 topics 字段中（JSON 字符串格式）
- **match**: MyMemory API 返回的翻译质量评分，范围 0-1，表示翻译的可信度

## Bug Details

### Fault Condition

缺陷在以下四种情况下表现：

1. **重试缺失**: 当 MyMemory API 调用因网络超时、API 限流等临时原因失败时，TranslationService 直接捕获异常并返回英文原文，没有重试机制
2. **分类标签缺失**: 当翻译的项目简介显示给用户时，只显示翻译后的文本，没有根据项目的 topics 添加智能分类标签
3. **质量阈值过严**: 当翻译质量评分（match）低于 0.3 时直接返回原文，这个阈值可能过于严格
4. **缓存无界**: 使用 ConcurrentHashMap 作为缓存，没有大小限制，可能导致内存无限增长

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type TranslationRequest {
    text: String,
    apiResponse: MyMemoryResponse (nullable),
    topics: List<String> (nullable),
    exception: Exception (nullable)
  }
  OUTPUT: boolean
  
  RETURN (input.exception != null AND retryCount == 0)  // Bug 1: 无重试
         OR (input.apiResponse != null AND input.topics != null AND NOT hasCategoryLabel(input.apiResponse.translatedText))  // Bug 2: 无分类标签
         OR (input.apiResponse != null AND input.apiResponse.match < 0.3 AND input.apiResponse.match >= 0.2)  // Bug 3: 阈值过严
         OR (cacheSize > 10000)  // Bug 4: 缓存无界
END FUNCTION
```

### Examples

- **Bug 1 - 无重试**: 调用 `translateToChinese("A powerful AI agent framework")` 时 MyMemory API 返回 503 Service Unavailable，系统直接返回 "A powerful AI agent framework"，没有重试
- **Bug 2 - 无分类标签**: 翻译 topics 包含 `["cursor-rules", "ai-coding"]` 的项目简介 "Collection of cursor rules"，返回 "Cursor 规则集合"，但应该返回 "【Cursor 规则集】Cursor 规则集合"
- **Bug 3 - 阈值过严**: MyMemory API 返回 match=0.25 的翻译 "AI 代理框架"，系统返回英文原文 "AI agent framework"，但 0.25 的质量评分实际上是可接受的
- **Bug 4 - 缓存无界**: 系统运行一周后，translationCache 包含 50000 个条目，占用大量内存，但没有淘汰机制

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- 空值和空字符串输入必须继续直接返回，不进行翻译（Requirements 3.1）
- 超过 1000 字符的文本必须继续截断到 1000 字符后再翻译（Requirements 3.2）
- 缓存命中时必须继续直接返回缓存结果，不重复调用 API（Requirements 3.3）
- GitHub 爬虫必须继续只翻译 description 字段，不翻译 README 内容（Requirements 3.4）
- 项目的 star 数或 fork 数更新时必须继续更新这些字段，但不重新翻译已有的 description（Requirements 3.5）
- 必须继续使用免费的 MyMemory API，保持零预算约束（Requirements 3.6）

**Scope:**
所有不涉及以下情况的输入应该完全不受此修复影响：
- API 调用失败的情况（将添加重试）
- 需要分类标签的翻译结果（将添加标签）
- match 在 0.2-0.3 之间的翻译（将调整阈值）
- 缓存大小超过限制的情况（将使用 LRU）

## Hypothesized Root Cause

基于缺陷描述，最可能的问题是：

1. **缺少重试逻辑**: TranslationService.translateWithMyMemory() 方法中，所有异常都被简单捕获并返回原文，没有实现指数退避重试机制
   - 当前代码：`catch (Exception e) { log.warn("Translation failed: {}", e.getMessage()); } return trimmedText;`
   - 缺少重试计数器和延迟逻辑

2. **缺少分类标签生成**: GitHubCrawlerService.parseSkill() 方法中，虽然提取了 topics 字段，但 TranslationService 不接收 topics 参数，无法根据 topics 生成分类标签
   - 当前代码：`String translatedDescription = translationService.translateToChinese(originalDescription);`
   - TranslationService 不知道项目的 topics 信息

3. **质量阈值硬编码**: TranslationService.translateWithMyMemory() 中硬编码 `if (match < 0.3)` 阈值
   - 这个阈值可能过于保守，导致一些可用的翻译被丢弃
   - 缺少日志记录低质量翻译以便后续分析

4. **使用无界 ConcurrentHashMap**: TranslationService 使用 `private final Map<String, String> translationCache = new ConcurrentHashMap<>();`
   - ConcurrentHashMap 没有大小限制，长时间运行会导致内存泄漏
   - 没有 LRU 淘汰策略

## Correctness Properties

Property 1: Fault Condition - API 失败重试和智能分类标签

_For any_ 翻译请求，当 MyMemory API 调用失败时，修复后的 translateWithMyMemory 方法 SHALL 自动重试最多 3 次（间隔分别为 1 秒、2 秒、4 秒），只有所有重试都失败才返回原文；当翻译成功且提供了 topics 参数时，修复后的方法 SHALL 根据 topics 自动判断项目类型并在翻译文本前添加合适的中文分类标签（如"【AI 提示词工具】"、"【Cursor 规则集】"、"【AI 代理】"等）。

**Validates: Requirements 2.1, 2.2**

Property 2: Fault Condition - 质量阈值和缓存限制

_For any_ 翻译请求，当 MyMemory API 返回的 match 评分在 0.2-0.3 之间时，修复后的 translateWithMyMemory 方法 SHALL 接受该翻译（而不是返回原文），并在日志中记录低质量翻译以便后续优化；修复后的 TranslationService SHALL 使用带有大小限制（最大 10000 条目）的 LRU 缓存机制，防止内存无限增长。

**Validates: Requirements 2.3, 2.4**

Property 3: Preservation - 现有行为保持不变

_For any_ 输入，当输入为 null、空字符串、超过 1000 字符、已在缓存中、或不涉及 API 失败/分类标签/质量阈值/缓存大小问题时，修复后的代码 SHALL 产生与原始代码完全相同的行为，保持空值处理、文本截断、缓存命中逻辑、只翻译 description 字段、不重新翻译已有内容、继续使用 MyMemory API 等现有功能。

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

假设我们的根因分析正确：

**File**: `backend/src/main/java/com/skillsfinder/service/TranslationService.java`

**Function**: `translateWithMyMemory`, `translateToChinese`

**Specific Changes**:
1. **添加重试机制**: 在 translateWithMyMemory() 方法中实现指数退避重试
   - 添加重试循环，最多重试 3 次
   - 每次重试间隔递增：1 秒、2 秒、4 秒
   - 只在所有重试都失败后才返回原文
   - 记录每次重试的日志

2. **添加分类标签生成**: 创建新方法 addCategoryLabel() 和修改 translateToChinese() 签名
   - 添加 `translateToChinese(String text, List<String> topics)` 重载方法
   - 创建 `addCategoryLabel(String translatedText, List<String> topics)` 方法
   - 根据 topics 匹配规则生成分类标签：
     - topics 包含 "cursor-rules" → "【Cursor 规则集】"
     - topics 包含 "gpt-prompts" 或 "system-prompt" → "【AI 提示词工具】"
     - topics 包含 "ai-agent" → "【AI 代理】"
     - topics 包含 "claude" → "【Claude 工具】"
   - 将标签前置到翻译文本前

3. **调整质量阈值**: 修改 translateWithMyMemory() 中的阈值判断
   - 将阈值从 0.3 降低到 0.2
   - 添加日志记录：当 match < 0.3 时记录警告日志，包含原文、译文和评分
   - 保持阈值可配置性（可选：通过 @Value 注入）

4. **引入 LRU 缓存**: 替换 ConcurrentHashMap 为 LRU 缓存实现
   - 使用 LinkedHashMap 实现 LRU 缓存，最大容量 10000
   - 重写 removeEldestEntry() 方法实现自动淘汰
   - 使用 Collections.synchronizedMap() 保证线程安全
   - 或者使用 Guava Cache（如果项目已依赖 Guava）

5. **修改 GitHubCrawlerService**: 更新调用方式传递 topics
   - 在 parseSkill() 方法中，解析 topics JSON 字符串为 List<String>
   - 调用 `translationService.translateToChinese(originalDescription, topicsList)`
   - 保持向后兼容：如果 topics 为空，使用原有的单参数方法

**File**: `backend/src/main/java/com/skillsfinder/service/GitHubCrawlerService.java`

**Function**: `parseSkill`

**Specific Changes**:
1. **传递 topics 参数**: 修改翻译服务调用
   - 解析 `item.path("topics")` 为 List<String>
   - 调用 `translationService.translateToChinese(originalDescription, topicsList)`

## Testing Strategy

### Validation Approach

测试策略遵循两阶段方法：首先在未修复的代码上运行探索性测试以暴露缺陷的反例，然后验证修复后的代码正确工作并保持现有行为不变。

### Exploratory Fault Condition Checking

**Goal**: 在实施修复之前，在未修复的代码上暴露缺陷的反例。确认或反驳根因分析。如果反驳，需要重新假设。

**Test Plan**: 编写测试模拟 API 失败、不同 topics 组合、不同 match 评分、大量缓存条目等场景，并断言当前代码的缺陷行为。在未修复的代码上运行这些测试以观察失败并理解根因。

**Test Cases**:
1. **API 失败无重试测试**: 模拟 MyMemory API 返回 503 错误，验证当前代码直接返回原文而不重试（将在未修复代码上通过，因为这是当前行为）
2. **分类标签缺失测试**: 翻译包含 topics=["cursor-rules"] 的项目简介，验证返回的翻译不包含"【Cursor 规则集】"标签（将在未修复代码上通过）
3. **质量阈值过严测试**: 模拟 MyMemory API 返回 match=0.25 的翻译，验证当前代码返回原文而不是翻译（将在未修复代码上通过）
4. **缓存无界测试**: 添加 15000 个翻译到缓存，验证缓存大小超过 10000 且没有淘汰（将在未修复代码上通过）

**Expected Counterexamples**:
- API 失败时没有重试日志，直接返回原文
- 翻译结果不包含基于 topics 的分类标签
- match=0.25 的翻译被拒绝，返回原文
- 缓存大小无限增长，没有 LRU 淘汰

### Fix Checking

**Goal**: 验证对于所有满足缺陷条件的输入，修复后的函数产生期望的行为。

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := translateToChinese_fixed(input.text, input.topics)
  ASSERT expectedBehavior(result, input)
END FOR

FUNCTION expectedBehavior(result, input)
  IF input.exception != null THEN
    ASSERT retryCount == 3  // 重试了 3 次
    ASSERT result == input.text OR result contains translation  // 重试成功或失败后返回原文
  END IF
  
  IF input.topics != null AND input.topics contains known category THEN
    ASSERT result starts with category label  // 包含分类标签
  END IF
  
  IF input.apiResponse.match >= 0.2 AND input.apiResponse.match < 0.3 THEN
    ASSERT result == input.apiResponse.translatedText  // 接受低质量翻译
    ASSERT log contains warning about low quality  // 记录警告日志
  END IF
  
  IF cacheSize > 10000 THEN
    ASSERT oldest entry was removed  // LRU 淘汰最旧条目
  END IF
END FUNCTION
```

### Preservation Checking

**Goal**: 验证对于所有不满足缺陷条件的输入，修复后的函数产生与原始函数相同的结果。

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT translateToChinese_original(input.text) = translateToChinese_fixed(input.text, null)
END FOR
```

**Testing Approach**: 推荐使用基于属性的测试进行保持性检查，因为：
- 它自动生成跨输入域的许多测试用例
- 它捕获手动单元测试可能遗漏的边缘情况
- 它为所有非缺陷输入提供强有力的保证，确保行为不变

**Test Plan**: 首先在未修复的代码上观察正常输入的行为，然后编写基于属性的测试捕获该行为。

**Test Cases**:
1. **空值处理保持**: 观察未修复代码对 null 和空字符串的处理，验证修复后行为相同
2. **文本截断保持**: 观察未修复代码对超过 1000 字符文本的截断，验证修复后行为相同
3. **缓存命中保持**: 观察未修复代码的缓存命中逻辑，验证修复后缓存命中时不调用 API
4. **正常翻译保持**: 对于 match >= 0.3 的正常翻译，验证修复后行为与原代码相同
5. **无 topics 调用保持**: 调用单参数 translateToChinese(text) 方法，验证行为与原代码相同（不添加标签）

### Unit Tests

- 测试重试机制：模拟 API 失败 2 次后成功，验证重试逻辑和延迟
- 测试分类标签生成：测试各种 topics 组合生成正确的标签
- 测试质量阈值：测试 match=0.2, 0.25, 0.3, 0.35 等不同评分的处理
- 测试 LRU 缓存：添加 10001 个条目，验证最旧的条目被淘汰
- 测试边缘情况：空 topics、未知 topics、多个匹配的 topics

### Property-Based Tests

- 生成随机文本和 topics 组合，验证翻译结果格式正确（标签 + 翻译文本）
- 生成随机 API 响应（不同 match 评分），验证阈值逻辑正确
- 生成大量随机翻译请求，验证缓存大小始终 <= 10000
- 生成随机输入（包括 null、空字符串、长文本），验证保持性属性

### Integration Tests

- 测试完整的 GitHub 爬虫流程：爬取项目 → 翻译 description → 添加分类标签 → 保存到数据库
- 测试 API 临时故障场景：模拟网络抖动，验证重试机制使翻译最终成功
- 测试长时间运行场景：模拟系统运行一周，验证缓存大小稳定在 10000 以下
- 测试不同项目类型：验证 cursor-rules、gpt-prompts、ai-agent 等项目获得正确的分类标签
