<template>
  <router-link :to="`/skill/${skill.id}`" class="skill-card">
    <div class="card-header">
      <img :src="skill.avatarUrl" :alt="skill.owner" class="owner-avatar" />
      <div class="card-meta">
        <span class="owner-name">{{ skill.owner }}</span>
        <span class="repo-name">/{{ skill.repository }}</span>
      </div>
    </div>
    
    <h3 class="skill-name">{{ skill.name }}</h3>
    <p class="skill-desc">{{ skill.description || '暂无描述' }}</p>
    
    <div class="card-footer">
      <div class="tags" v-if="skill.topics">
        <span 
          v-for="topic in parseTopics(skill.topics).slice(0, 3)" 
          :key="topic" 
          class="tag"
        >
          {{ topic }}
        </span>
      </div>
      <div class="stats">
        <span class="stat">
          <span class="stat-icon">⭐</span>
          {{ formatNumber(skill.starCount) }}
        </span>
        <span v-if="skill.language" class="stat language">
          {{ skill.language }}
        </span>
      </div>
    </div>
  </router-link>
</template>

<script setup>
defineProps({
  skill: {
    type: Object,
    required: true
  }
})

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
</script>

<style scoped>
.skill-card {
  display: block;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 1.5rem;
  text-decoration: none;
  transition: all 0.3s ease;
  height: 100%;
}

.skill-card:hover {
  transform: translateY(-4px);
  border-color: rgba(96, 165, 250, 0.3);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.owner-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  overflow: hidden;
}

.owner-name {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
}

.repo-name {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.skill-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #fff;
  margin-bottom: 0.5rem;
  line-height: 1.4;
}

.skill-desc {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 1.25rem;
  flex: 1;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.tag {
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
  border-radius: 6px;
}

.stats {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.stat {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.6);
}

.stat-icon {
  font-size: 0.875rem;
}

.language {
  padding: 0.2rem 0.5rem;
  background: rgba(167, 139, 250, 0.15);
  color: #a78bfa;
  border-radius: 4px;
  font-size: 0.75rem;
}
</style>
