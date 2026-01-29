<template>
  <div class="ancient-layout">
    <aside class="ancient-sidebar">
      <div class="ancient-sidebar-title">
        考优爬虫
      </div>
      
      <nav class="ancient-nav">
        <router-link
          to="/clients"
          class="ancient-nav-item"
          :class="{ active: $route.path === '/clients' }"
        >
          客户端管理
        </router-link>
        
        <router-link
          to="/scripts"
          class="ancient-nav-item"
          :class="{ active: $route.path === '/scripts' }"
        >
          脚本管理
        </router-link>
        
        <router-link
          to="/statistics"
          class="ancient-nav-item"
          :class="{ active: $route.path === '/statistics' }"
        >
          统计分析
        </router-link>
      </nav>
      
      <div class="user-info">
        <div class="user-name">{{ userStore.username }}</div>
        <button @click="handleLogout" class="ancient-button secondary logout-button">
          退出登录
        </button>
      </div>
    </aside>
    
    <main class="ancient-main">
      <header class="ancient-header">
        <h2 class="ancient-header-title">{{ pageTitle }}</h2>
        <div class="header-actions">
          <span class="current-time">{{ currentTime }}</span>
        </div>
      </header>
      
      <div class="content-wrapper">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentTime = ref('')
let timeInterval = null

const pageTitle = computed(() => {
  const titles = {
    '/clients': '客户端管理',
    '/scripts': '脚本管理',
    '/statistics': '统计分析'
  }
  return titles[route.path] || '考优爬虫管理系统'
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

const handleLogout = async () => {
  try {
    await authApi.logout({
      clientId: 'web-client-' + Date.now()
    })
  } catch (err) {
    console.error('Logout error:', err)
  } finally {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
.ancient-nav {
  margin-bottom: 2rem;
}

.user-info {
  margin-top: auto;
  padding-top: 1rem;
  border-top: 2px solid rgba(255, 255, 255, 0.2);
}

.user-name {
  text-align: center;
  margin-bottom: 1rem;
  font-size: 1.1rem;
  font-weight: bold;
}

.logout-button {
  width: 100%;
  padding: 8px 16px;
  font-size: 0.9rem;
}

.current-time {
  color: var(--light-text);
  font-size: 0.9rem;
}

.content-wrapper {
  padding: 1rem;
}
</style>
