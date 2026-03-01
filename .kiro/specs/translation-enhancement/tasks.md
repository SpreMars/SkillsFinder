# Implementation Plan

- [x] 1. 编写缺陷条件探索测试
  - **Property 1: Fault Condition** - API 失败重试和智能分类标签
  - **关键**: 此测试必须在未修复的代码上失败 - 失败确认缺陷存在
  - **不要在测试失败时尝试修复测试或代码**
  - **注意**: 此测试编码了期望的行为 - 在实现后通过时将验证修复
  - **目标**: 暴露证明缺陷存在的反例
  - **作用域 PBT 方法**: 对于确定性缺陷，将属性作用域限定为具体的失败案例以确保可重现性
  - 测试实现细节来自设计中的 Fault Condition
  - 测试断言应匹配设计中的 Expected Behavior Properties
  - 在未修复的代码上运行测试
  - **预期结果**: 测试失败（这是正确的 - 证明缺陷存在）
  - 记录发现的反例以理解根本原因
  - 当测试编写、运行并记录失败时标记任务完成
  - 测试案例：
    - API 失败无重试：模拟 MyMemory API 返回 503 错误，验证当前代码直接返回原文而不重试
    - 分类标签缺失：翻译包含 topics=["cursor-rules"] 的项目简介，验证返回的翻译不包含"【Cursor 规则集】"标签
    - 质量阈值过严：模拟 MyMemory API 返回 match=0.25 的翻译，验证当前代码返回原文而不是翻译
    - 缓存无界：添加 15000 个翻译到缓存，验证缓存大小超过 10000 且没有淘汰
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 2. 编写保持性属性测试（在实现修复之前）
  - **Property 2: Preservation** - 现有行为保持不变
  - **重要**: 遵循观察优先方法
  - 在未修复的代码上观察非缺陷输入的行为
  - 编写基于属性的测试捕获从 Preservation Requirements 观察到的行为模式
  - 基于属性的测试生成许多测试用例以提供更强的保证
  - 在未修复的代码上运行测试
  - **预期结果**: 测试通过（这确认了要保持的基线行为）
  - 当测试编写、运行并在未修复代码上通过时标记任务完成
  - 测试案例：
    - 空值处理保持：验证 null 和空字符串直接返回
    - 文本截断保持：验证超过 1000 字符的文本被截断
    - 缓存命中保持：验证缓存命中时不调用 API
    - 正常翻译保持：验证 match >= 0.3 的翻译正常工作
    - 无 topics 调用保持：验证单参数方法不添加标签
    - MyMemory API 使用保持：验证继续使用免费的 MyMemory API
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 3. 修复翻译服务增强

  - [x] 3.1 实现 API 失败重试机制
    - 在 TranslationService.translateWithMyMemory() 方法中添加重试循环
    - 实现指数退避：最多重试 3 次，间隔分别为 1 秒、2 秒、4 秒
    - 使用 Thread.sleep() 实现延迟（或 Spring Retry 如果可用）
    - 记录每次重试的日志（包括重试次数和原因）
    - 只在所有重试都失败后才返回原文
    - _Bug_Condition: input.exception != null AND retryCount == 0_
    - _Expected_Behavior: 重试最多 3 次，间隔递增_
    - _Preservation: 保持空值处理、文本截断、缓存命中逻辑_
    - _Requirements: 2.1, 3.1, 3.2, 3.3_

  - [x] 3.2 实现智能分类标签生成
    - 在 TranslationService 中添加 translateToChinese(String text, List<String> topics) 重载方法
    - 创建 addCategoryLabel(String translatedText, List<String> topics) 私有方法
    - 实现分类规则：
      - topics 包含 "cursor-rules" → "【Cursor 规则集】"
      - topics 包含 "gpt-prompts" 或 "system-prompt" → "【AI 提示词工具】"
      - topics 包含 "ai-agent" → "【AI 代理】"
      - topics 包含 "claude" → "【Claude 工具】"
    - 将标签前置到翻译文本前
    - 保持向后兼容：单参数方法不添加标签
    - _Bug_Condition: input.topics != null AND NOT hasCategoryLabel(translatedText)_
    - _Expected_Behavior: 根据 topics 添加合适的分类标签_
    - _Preservation: 保持无 topics 调用的原有行为_
    - _Requirements: 2.2, 3.4_

  - [x] 3.3 调整翻译质量阈值
    - 将 translateWithMyMemory() 中的阈值从 0.3 降低到 0.2
    - 添加日志记录：当 match < 0.3 时记录警告日志
    - 警告日志应包含原文、译文和评分
    - 可选：通过 @Value 注入使阈值可配置
    - _Bug_Condition: match < 0.3 AND match >= 0.2_
    - _Expected_Behavior: 接受 match >= 0.2 的翻译并记录警告_
    - _Preservation: 保持 match >= 0.3 的正常翻译行为_
    - _Requirements: 2.3_

  - [x] 3.4 引入 LRU 缓存机制
    - 替换 ConcurrentHashMap 为 LRU 缓存实现
    - 使用 LinkedHashMap 实现 LRU，最大容量 10000
    - 重写 removeEldestEntry() 方法实现自动淘汰
    - 使用 Collections.synchronizedMap() 保证线程安全
    - 或者使用 Guava Cache（如果项目已依赖 Guava）
    - _Bug_Condition: cacheSize > 10000_
    - _Expected_Behavior: 缓存大小限制在 10000，LRU 淘汰最旧条目_
    - _Preservation: 保持缓存命中逻辑不变_
    - _Requirements: 2.4, 3.3_

  - [x] 3.5 更新 GitHubCrawlerService 调用方式
    - 在 parseSkill() 方法中解析 topics JSON 字符串为 List<String>
    - 调用 translationService.translateToChinese(originalDescription, topicsList)
    - 处理 topics 为空或 null 的情况
    - 保持向后兼容：如果 topics 为空，传递 null 或空列表
    - _Preservation: 保持只翻译 description 字段，不翻译 README_
    - _Requirements: 2.2, 3.4_

  - [x] 3.6 验证缺陷条件探索测试现在通过
    - **Property 1: Expected Behavior** - API 失败重试和智能分类标签
    - **重要**: 重新运行任务 1 中的相同测试 - 不要编写新测试
    - 任务 1 中的测试编码了期望的行为
    - 当此测试通过时，确认期望的行为得到满足
    - 运行任务 1 中的缺陷条件探索测试
    - **预期结果**: 测试通过（确认缺陷已修复）
    - _Requirements: Expected Behavior Properties from design_

  - [x] 3.7 验证保持性测试仍然通过
    - **Property 2: Preservation** - 现有行为保持不变
    - **重要**: 重新运行任务 2 中的相同测试 - 不要编写新测试
    - 运行任务 2 中的保持性属性测试
    - **预期结果**: 测试通过（确认没有回归）
    - 确认修复后所有测试仍然通过（没有回归）

- [x] 4. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户
