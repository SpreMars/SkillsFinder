<template>
  <div class="detail-page">
    <div class="container" v-if="skill">
      <button class="back-btn" @click="$router.back()">
        ← 返回
      </button>
      
      <div class="detail-card">
        <div class="detail-header">
          <img :src="skill.avatarUrl" :alt="skill.owner" class="detail-avatar" />
          <div class="detail-title-wrap">
            <h1 class="detail-title">{{ skill.name }}</h1>
            <div class="detail-meta">
              <span class="owner">{{ skill.owner }}</span>
              <span class="separator">/</span>
              <span class="repo">{{ skill.repository }}</span>
            </div>
          </div>
          <a :href="skill.htmlUrl" target="_blank" class="github-btn">
            GitHub →
          </a>
        </div>
        
        <p class="detail-desc">{{ skill.description || '暂无描述' }}</p>
        
        <div class="detail-stats">
          <div class="stat-item">
            <span class="stat-icon">⭐</span>
            <span class="stat-value">{{ formatNumber(skill.starCount) }}</span>
            <span class="stat-label">Stars</span>
          </div>
          <div class="stat-item">
            <span class="stat-icon">🍴</span>
            <span class="stat-value">{{ formatNumber(skill.forkCount) }}</span>
            <span class="stat-label">Forks</span>
          </div>
          <div class="stat-item" v-if="skill.language">
            <span class="stat-icon">🔤</span>
            <span class="stat-value">{{ skill.language }}</span>
            <span class="stat-label">Language</span>
          </div>
        </div>
        
        <div class="topics-section" v-if="skill.topics">
          <h3 class="section-label">Topics</h3>
          <div class="topics">
            <span 
              v-for="topic in parseTopics(skill.topics)" 
              :key="topic" 
              class="topic-tag"
            >
              {{ topic }}
            </span>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>
    
    <div v-else class="not-found">
      <p>Skill 未找到</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const skill = ref(null)
const loading = ref(true)

const fetchSkill = async () => {
  loading.value = true
  try {
    const res = await axios.get(`/api/skills/${route.params.id}`)
    skill.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const parseTopics = (topics) => {
  try {
    return JSON.parse(topics)
  } catch {
    return []
  }
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

onMounted(() => {
  fetchSkill()
})
</script>

<style scoped>
.detail-page {
  background: #0a0a0f;
  min-height: 100vh;
  padding: 2rem;
}

.container {
  max-width: 900px;
  margin: 0 auto;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 2rem;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.detail-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 2rem;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}

.detail-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}

.detail-title-wrap {
  flex: 1;
}

.detail-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.25rem;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.owner {
  color: rgba(255, 255, 255, 0.7);
  font-size: 1rem;
}

.separator {
  color: rgba(255, 255, 255, 0.4);
}

.repo {
  color: rgba(255, 255, 255, 0.5);
  font-size: 1rem;
}

.github-btn {
  padding: 0.75rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-radius: 10px;
  text-decoration: none;
  transition: all 0.2s;
}

.github-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3);
}

.detail-desc {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.7;
  margin-bottom: 2rem;
}

.detail-stats {
  display: flex;
  gap: 2rem;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 12px;
  margin-bottom: 2rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stat-icon {
  font-size: 1.25rem;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #fff;
}

.stat-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.5);
}

.topics-section {
  margin-top: 1rem;
}

.section-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.topics {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.topic-tag {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
  border-radius: 8px;
  transition: all 0.2s;
}

.topic-tag:hover {
  background: rgba(96, 165, 250, 0.25);
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #60a5fa;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.not-found {
  text-align: center;
  padding: 4rem;
  color: rgba(255, 255, 255, 0.5);
}
</style>
