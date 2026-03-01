# Bugfix Requirements Document

## Introduction

本文档描述后端翻译服务的优化需求。当前翻译服务在处理 GitHub 项目简介时存在以下问题：翻译失败时缺乏重试机制导致用户看到英文原文、翻译结果缺少智能分类标签影响用户体验、翻译质量评估机制不完善。本次优化将改进这些问题，同时保持零预算约束（继续使用免费的 MyMemory API）。

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN MyMemory API 调用失败（网络超时、API 限流等）THEN 系统直接返回英文原文，不进行重试

1.2 WHEN 翻译的项目简介显示给用户 THEN 系统只显示翻译后的文本，没有基于项目类型的智能分类标签（如"【AI 提示词工具】"、"【Cursor 规则集】"等）

1.3 WHEN 翻译质量评分（match）低于 0.3 THEN 系统返回原文，但这个阈值可能过于严格，导致一些可用的翻译被丢弃

1.4 WHEN 翻译结果被缓存 THEN 系统使用简单的 ConcurrentHashMap，没有考虑缓存大小限制和过期策略

### Expected Behavior (Correct)

2.1 WHEN MyMemory API 调用失败 THEN 系统应该自动重试最多 3 次（每次间隔递增），如果所有重试都失败才返回原文

2.2 WHEN 翻译的项目简介显示给用户 THEN 系统应该根据项目的 topics 自动判断类型并在翻译文本前添加合适的中文分类标签（如"【AI 提示词工具】"、"【Cursor 规则集】"、"【AI 代理】"等）

2.3 WHEN 翻译质量评分（match）低于阈值 THEN 系统应该使用更合理的阈值（如 0.2），并在日志中记录低质量翻译以便后续优化

2.4 WHEN 翻译结果被缓存 THEN 系统应该使用带有大小限制的缓存机制（如 LRU 缓存），防止内存无限增长

### Unchanged Behavior (Regression Prevention)

3.1 WHEN 输入文本为 null 或空字符串 THEN 系统应该继续直接返回原值，不进行翻译

3.2 WHEN 输入文本长度超过 1000 字符 THEN 系统应该继续截断到 1000 字符后再翻译

3.3 WHEN 翻译成功且结果已在缓存中 THEN 系统应该继续直接从缓存返回，不重复调用 API

3.4 WHEN 爬取 GitHub 项目数据 THEN 系统应该继续只翻译 description 字段，不翻译 README 内容

3.5 WHEN 项目的 star 数或 fork 数更新 THEN 系统应该继续更新这些字段，但不重新翻译已有的 description

3.6 WHEN 使用 MyMemory API THEN 系统应该继续使用免费的 MyMemory API，保持零预算约束
