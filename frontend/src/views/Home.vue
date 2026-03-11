<template>
  <div class="home">
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">
          发现优质的
          <span class="gradient-text">Agent Skills</span>
        </h1>
        <p class="hero-desc">
          搜集整理 GitHub 上的优秀 AI Agent 提示词和工作流
        </p>
        
        <div class="search-box">
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="搜索 Agent Skills..." 
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </button>
        </div>
      </div>
    </section>
    
    <div class="trending-banner" @click="handleTrendingClick">
      <span class="trending-icon">🔥</span>
      <span class="trending-text">查看本周最热 Agent Skills 榜单</span>
      <span class="trending-arrow">→</span>
    </div>
    
    <router-link to="/dcm" class="dcm-banner">
      <span class="dcm-icon">🏥</span>
      <span class="dcm-text">医学影像查看器 - CT影像无损浏览</span>
      <span class="dcm-arrow">→</span>
    </router-link>
    
    <div v-if="showTrending" class="modal-overlay" @click="showTrending = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>🔥 每周热榜</h2>
          <button class="close-btn" @click="showTrending = false">×</button>
        </div>
        
        <div class="trending-tabs">
          <button 
            v-for="tab in tabs" 
            :key="tab.key"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.icon }} {{ tab.label }}
          </button>
        </div>
        
        <div class="trending-list">
          <div v-if="trendingLoading" class="loading">
            <div class="spinner"></div>
          </div>
          
          <template v-else-if="activeTab === 'total'">
            <div v-if="trendingData.total.length === 0" class="empty">暂无数据</div>
            <div 
              v-for="(skill, index) in trendingData.total" 
              :key="skill.id" 
              class="trending-item"
              @click="$router.push(`/skill/${skill.id}`)"
            >
              <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <img :src="skill.avatarUrl" class="trending-avatar" />
              <div class="trending-info">
                <h4>{{ skill.name }}</h4>
                <p>{{ skill.description || '暂无描述' }}</p>
              </div>
              <span class="trending-stars">⭐ {{ formatNumber(skill.starCount) }}</span>
            </div>
          </template>
          
          <template v-else-if="activeTab === 'growth'">
            <div v-if="trendingData.growth.length === 0" class="empty">暂无数据</div>
            <div 
              v-for="(skill, index) in trendingData.growth" 
              :key="skill.id" 
              class="trending-item"
              @click="$router.push(`/skill/${skill.id}`)"
            >
              <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <img :src="skill.avatarUrl" class="trending-avatar" />
              <div class="trending-info">
                <h4>{{ skill.name }}</h4>
                <p>{{ skill.description || '暂无描述' }}</p>
              </div>
              <span class="star-change" :class="{ positive: skill.starChange > 0 }">
                {{ skill.starChange > 0 ? '↑' : '' }}{{ skill.starChange || 0 }}
              </span>
            </div>
          </template>
          
          <template v-else-if="activeTab === 'new'">
            <div v-if="trendingData.new.length === 0" class="empty">暂无数据</div>
            <div 
              v-for="(skill, index) in trendingData.new" 
              :key="skill.id" 
              class="trending-item"
              @click="$router.push(`/skill/${skill.id}`)"
            >
              <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <img :src="skill.avatarUrl" class="trending-avatar" />
              <div class="trending-info">
                <h4>{{ skill.name }}</h4>
                <p class="new-badge">🆕 7天内新秀</p>
              </div>
              <span class="trending-stars">⭐ {{ formatNumber(skill.starCount) }}</span>
            </div>
          </template>
          
          <template v-else-if="activeTab === 'language'">
            <div class="language-selector">
              <button 
                v-for="lang in popularLanguages" 
                :key="lang"
                class="lang-btn"
                :class="{ active: selectedLanguage === lang }"
                @click="selectedLanguage = lang"
              >
                {{ lang }}
              </button>
            </div>
            <div v-if="!selectedLanguage" class="empty">请选择编程语言</div>
            <div v-else-if="!trendingData.language[selectedLanguage]?.length" class="empty">暂无数据</div>
            <div 
              v-else
              v-for="(skill, index) in trendingData.language[selectedLanguage]" 
              :key="skill.id" 
              class="trending-item"
              @click="$router.push(`/skill/${skill.id}`)"
            >
              <span class="rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <img :src="skill.avatarUrl" class="trending-avatar" />
              <div class="trending-info">
                <h4>{{ skill.name }}</h4>
                <p>{{ skill.description || '暂无描述' }}</p>
              </div>
              <span class="trending-stars">⭐ {{ formatNumber(skill.starCount) }}</span>
            </div>
          </template>
        </div>
      </div>
    </div>
    
    <section class="skills-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">{{ searchKeyword ? '搜索结果' : '热门 Skills' }}</h2>
          <span class="skill-count">{{ totalElements }} 个 skills</span>
        </div>
        
        <div v-if="loading" class="loading">
          <div class="skeleton-grid">
            <div v-for="n in 8" :key="n" class="skeleton-card"></div>
          </div>
        </div>
        
        <div v-else-if="skills.length === 0" class="empty">
          <p>暂无数据</p>
        </div>
        
        <div v-else class="skills-grid">
          <SkillCard 
            v-for="(skill, index) in skills" 
            :key="skill.id" 
            :skill="skill"
            :style="{ animationDelay: `${index * 0.05}s` }"
            class="skill-item"
          />
        </div>
        
        <div v-if="totalPages > 1" class="pagination">
          <button 
            class="page-btn" 
            :disabled="currentPage === 0"
            @click="changePage(currentPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button 
            class="page-btn"
            :disabled="currentPage >= totalPages - 1"
            @click="changePage(currentPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import SkillCard from '@/components/SkillCard.vue'

const skills = ref([])
const loading = ref(true)
const searchKeyword = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

const showTrending = ref(false)
const trendingSkills = ref([])
const trendingLoading = ref(false)
const activeTab = ref('total')
const selectedLanguage = ref('')

const tabs = [
  { key: 'total', label: '总榜', icon: '🏆' },
  { key: 'growth', label: '飙升', icon: '🚀' },
  { key: 'new', label: '新秀', icon: '🆕' },
  { key: 'language', label: '语言', icon: '💻' }
]

const trendingData = ref({
  total: [],
  growth: [],
  new: [],
  language: {}
})

const popularLanguages = ref([])

const fetchSkills = async () => {
  loading.value = true
  try {
    const params = new URLSearchParams()
    params.append('page', currentPage.value)
    params.append('size', 12)
    if (searchKeyword.value) params.append('keyword', searchKeyword.value)
    
    const res = await axios.get(`/api/skills?${params}`)
    skills.value = res.data.content
    totalPages.value = res.data.totalPages
    totalElements.value = res.data.totalElements
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchTrending = async () => {
  trendingLoading.value = true
  try {
    const res = await axios.get('/api/skills/trending')
    trendingData.value = res.data
    popularLanguages.value = Object.keys(res.data.language || {})
    if (popularLanguages.value.length > 0) {
      selectedLanguage.value = popularLanguages.value[0]
    }
  } catch (e) {
    console.error(e)
  } finally {
    trendingLoading.value = false
  }
}

const handleTrendingClick = () => {
  showTrending.value = true
  if (trendingData.value.total.length === 0) {
    fetchTrending()
  }
}

const handleSearch = () => {
  currentPage.value = 0
  fetchSkills()
}

const changePage = (page) => {
  currentPage.value = page
  fetchSkills()
  window.scrollTo({ top: 400, behavior: 'smooth' })
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

onMounted(() => {
  fetchSkills()
})
</script>

<style scoped>
.home {
  background: #0a0a0f;
  min-height: 100vh;
}

.hero {
  padding: 6rem 2rem 4rem;
  text-align: center;
  background: radial-gradient(ellipse at top, rgba(96, 165, 250, 0.15) 0%, transparent 60%);
}

.hero-content {
  max-width: 700px;
  margin: 0 auto;
}

.hero-title {
  font-size: 3rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 1rem;
  line-height: 1.2;
}

.gradient-text {
  background: linear-gradient(135deg, #60a5fa, #a78bfa, #f472b6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 1.125rem;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 2.5rem;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 0;
  max-width: 560px;
  margin: 0 auto 1.5rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 0.5rem;
  transition: all 0.3s;
}

.search-box:focus-within {
  border-color: rgba(96, 165, 250, 0.5);
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.1), 0 8px 32px rgba(0, 0, 0, 0.3);
}

.search-input {
  flex: 1;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  background: transparent;
  border: none;
  color: #fff;
  outline: none;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
  border-radius: 12px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.search-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
}

.search-icon {
  width: 20px;
  height: 20px;
}

.trending-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem 1.5rem;
  margin: -2rem auto 2rem;
  max-width: 500px;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.15), rgba(239, 68, 68, 0.15));
  border: 1px solid rgba(251, 146, 60, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.trending-banner:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(251, 146, 60, 0.2);
  border-color: rgba(251, 146, 60, 0.5);
}

.trending-icon {
  font-size: 1.25rem;
}

.trending-text {
  color: #fb923c;
  font-weight: 500;
}

.trending-arrow {
  color: #fb923c;
  transition: transform 0.2s;
}

.trending-banner:hover .trending-arrow {
  transform: translateX(4px);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: rgba(20, 20, 28, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  width: 90%;
  max-width: 700px;
  max-height: 80vh;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { 
    opacity: 0;
    transform: translateY(20px);
  }
  to { 
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header h2 {
  color: #fff;
  font-size: 1.25rem;
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-size: 1.25rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.trending-tabs {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  overflow-x: auto;
}

.tab-btn {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.tab-btn.active {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border-color: transparent;
  color: #fff;
}

.language-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.lang-btn {
  padding: 0.375rem 0.75rem;
  font-size: 0.8rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s;
}

.lang-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.lang-btn.active {
  background: rgba(59, 130, 246, 0.3);
  border-color: #3b82f6;
  color: #60a5fa;
}

.star-change {
  font-size: 0.875rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
}

.star-change.positive {
  color: #4ade80;
}

.new-badge {
  color: #4ade80 !important;
  font-size: 0.75rem !important;
}

.trending-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  margin-bottom: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
}

.trending-item:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(251, 146, 60, 0.3);
  transform: translateX(4px);
}

.rank {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.9rem;
}

.rank-1 {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #000;
}

.rank-2 {
  background: linear-gradient(135deg, #94a3b8, #64748b);
  color: #fff;
}

.rank-3 {
  background: linear-gradient(135deg, #cd7f32, #b45309);
  color: #fff;
}

.rank:not(.rank-1):not(.rank-2):not(.rank-3) {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.6);
}

.trending-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.trending-info {
  flex: 1;
  min-width: 0;
}

.trending-info h4 {
  color: #fff;
  font-size: 1rem;
  margin: 0 0 0.25rem 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trending-info p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.8rem;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trending-stars {
  color: #fb923c;
  font-size: 0.875rem;
  font-weight: 500;
  white-space: nowrap;
}

.skills-section {
  padding: 3rem 2rem 4rem;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #fff;
}

.skill-count {
  color: rgba(255, 255, 255, 0.5);
  font-size: 0.9rem;
}

.skills-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.skill-item {
  animation: fadeInUp 0.5s ease-out both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.loading {
  min-height: 400px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.skeleton-card {
  height: 220px;
  background: linear-gradient(90deg, rgba(255,255,255,0.03) 25%, rgba(255,255,255,0.06) 50%, rgba(255,255,255,0.03) 75%);
  background-size: 200% 100%;
  border-radius: 16px;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.empty {
  text-align: center;
  padding: 4rem;
  color: rgba(255, 255, 255, 0.5);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 3rem;
}

.page-btn {
  padding: 0.625rem 1.25rem;
  font-size: 0.9rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.9rem;
}

.dcm-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin: 1.5rem 2rem;
  padding: 1.25rem 2rem;
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.15), rgba(99, 102, 241, 0.15));
  border: 1px solid rgba(34, 211, 238, 0.3);
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.3s ease;
}

.dcm-banner:hover {
  background: linear-gradient(135deg, rgba(34, 211, 238, 0.25), rgba(99, 102, 241, 0.25));
  border-color: rgba(34, 211, 238, 0.5);
  transform: translateY(-2px);
}

.dcm-icon {
  font-size: 1.5rem;
}

.dcm-text {
  flex: 1;
  font-size: 1rem;
  font-weight: 500;
  color: #fff;
}

.dcm-arrow {
  color: rgba(255, 255, 255, 0.6);
  font-size: 1.25rem;
}
</style>
