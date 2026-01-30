<template>
  <div class="tech-layout">
    <aside class="tech-sidebar">
      <div class="tech-sidebar-title">
        考优爬虫
      </div>
      
      <nav class="tech-nav">
        <router-link
          to="/clients"
          class="tech-nav-item"
          :class="{ active: $route.path === '/clients' }"
        >
          客户端管理
        </router-link>
        
        <router-link
          to="/scripts"
          class="tech-nav-item"
          :class="{ active: $route.path === '/scripts' }"
        >
          脚本管理
        </router-link>
        
        <router-link
          to="/statistics"
          class="tech-nav-item"
          :class="{ active: $route.path === '/statistics' }"
        >
          统计分析
        </router-link>
      </nav>
      
      <div class="user-info">
        <div class="user-name">{{ userStore.username }}</div>
        <button @click="handleLogout" class="tech-button tech-button-secondary logout-button">
          退出登录
        </button>
      </div>
    </aside>
    
    <main class="tech-main">
      <header class="tech-header">
        <h2 class="tech-header-title">{{ pageTitle }}</h2>
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
.tech-layout {
  display: flex;
  min-height: 100vh;
}

.tech-sidebar {
  width: 250px;
  display: flex;
  flex-direction: column;
  padding: 2rem 1rem;
  background: rgba(10, 10, 15, 0.95);
  border-right: 1px solid var(--border-color);
  position: relative;
  backdrop-filter: blur(10px);
}

.tech-sidebar::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(180deg, transparent, var(--primary-color), transparent);
}

.tech-sidebar-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: var(--primary-color);
  text-align: center;
  margin-bottom: 2rem;
  text-shadow: 0 0 10px var(--glow-color);
  letter-spacing: 2px;
  text-transform: uppercase;
}

.tech-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.tech-nav-item {
  padding: 1rem;
  color: var(--light-text);
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  display: block;
}

.tech-nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--primary-color);
  transform: scaleY(0);
  transition: all 0.3s ease;
}

.tech-nav-item:hover,
.tech-nav-item.active {
  background: rgba(0, 240, 255, 0.1);
  color: var(--primary-color);
}

.tech-nav-item:hover::before,
.tech-nav-item.active::before {
  transform: scaleY(1);
}

.tech-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0a0a0f 0%, #1a1a2e 50%, #0a0a0f 100%);
  position: relative;
  overflow: hidden;
}

.tech-main::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(0, 240, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 240, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: gridMove 20s linear infinite;
  pointer-events: none;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

.tech-header {
  background: rgba(10, 10, 15, 0.95);
  border-bottom: 2px solid var(--border-color);
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 10;
  backdrop-filter: blur(10px);
}

.tech-header::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--primary-color), transparent);
  animation: headerGlow 3s ease-in-out infinite;
}

@keyframes headerGlow {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.tech-header-title {
  font-size: 1.5rem;
  color: var(--primary-color);
  text-shadow: 0 0 10px var(--glow-color);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.user-info {
  margin-top: auto;
  padding-top: 1rem;
  border-top: 2px solid rgba(0, 240, 255, 0.2);
}

.user-name {
  text-align: center;
  margin-bottom: 1rem;
  font-size: 1.1rem;
  font-weight: bold;
  color: var(--text-color);
}

.logout-button {
  width: 100%;
  padding: 0.8rem 1rem;
  font-size: 0.9rem;
}

.current-time {
  color: var(--light-text);
  font-size: 0.9rem;
  font-family: 'Courier New', monospace;
  letter-spacing: 1px;
}

.content-wrapper {
  padding: 2rem;
  position: relative;
  z-index: 1;
}
</style>
