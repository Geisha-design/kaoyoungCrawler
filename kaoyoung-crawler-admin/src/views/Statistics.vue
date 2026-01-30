<template>
  <div class="statistics-page">
    <div class="tech-grid tech-grid-4 statistics-grid">
      <div class="tech-stat-card stat-card">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <h4 class="stat-title">总客户端数</h4>
          <div class="tech-stat-value stat-value">{{ statistics.totalClients || 0 }}</div>
        </div>
      </div>
      
      <div class="tech-stat-card stat-card">
        <div class="stat-icon">🟢</div>
        <div class="stat-content">
          <h4 class="stat-title">在线客户端</h4>
          <div class="tech-stat-value stat-value">{{ statistics.onlineClients || 0 }}</div>
        </div>
      </div>
      
      <div class="tech-stat-card stat-card">
        <div class="stat-icon">🟡</div>
        <div class="stat-content">
          <h4 class="stat-title">空闲客户端</h4>
          <div class="tech-stat-value stat-value">{{ statistics.idleClients || 0 }}</div>
        </div>
      </div>
      
      <div class="tech-stat-card stat-card">
        <div class="stat-icon">📈</div>
        <div class="stat-content">
          <h4 class="stat-title">忙碌客户端</h4>
          <div class="tech-stat-value stat-value">{{ (statistics.onlineClients || 0) - (statistics.idleClients || 0) }}</div>
        </div>
      </div>
    </div>
    
    <div class="tech-card charts-card">
      <div class="card-header">
        <h3 class="card-title">系统概览</h3>
        <button @click="refreshStatistics" class="tech-button tech-button-secondary refresh-button">
          刷新
        </button>
      </div>
      
      <div class="charts-content">
        <div class="chart-section">
          <h4 class="section-title">客户端状态分布</h4>
          <div class="chart-bar">
            <div class="bar-label">在线</div>
            <div class="bar-container">
              <div
                class="tech-progress-bar bar-fill online-bar"
                :style="{ width: getPercentage(statistics.onlineClients, statistics.totalClients) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ statistics.onlineClients || 0 }}</div>
          </div>
          
          <div class="chart-bar">
            <div class="bar-label">离线</div>
            <div class="bar-container">
              <div
                class="tech-progress-bar bar-fill offline-bar"
                :style="{ width: getPercentage(statistics.totalClients - statistics.onlineClients, statistics.totalClients) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ (statistics.totalClients || 0) - (statistics.onlineClients || 0) }}</div>
          </div>
        </div>
        
        <div class="chart-section">
          <h4 class="section-title">客户端工作状态</h4>
          <div class="chart-bar">
            <div class="bar-label">空闲</div>
            <div class="bar-container">
              <div
                class="tech-progress-bar bar-fill idle-bar"
                :style="{ width: getPercentage(statistics.idleClients, statistics.onlineClients) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ statistics.idleClients || 0 }}</div>
          </div>
          
          <div class="chart-bar">
            <div class="bar-label">忙碌</div>
            <div class="bar-container">
              <div
                class="tech-progress-bar bar-fill busy-bar"
                :style="{ width: getPercentage(statistics.onlineClients - statistics.idleClients, statistics.onlineClients) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ (statistics.onlineClients || 0) - (statistics.idleClients || 0) }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="tech-card info-card">
      <div class="card-header">
        <h3 class="card-title">系统信息</h3>
      </div>
      
      <div class="info-content">
        <div class="info-item">
          <span class="info-label">系统名称:</span>
          <span class="info-value">考优爬虫管理系统</span>
        </div>
        
        <div class="info-item">
          <span class="info-label">系统版本:</span>
          <span class="info-value">v1.0.0</span>
        </div>
        
        <div class="info-item">
          <span class="info-label">当前用户:</span>
          <span class="info-value">{{ userStore.username }}</span>
        </div>
        
        <div class="info-item">
          <span class="info-label">最后更新:</span>
          <span class="info-value">{{ lastUpdateTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { adminApi } from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

const statistics = ref({
  totalClients: 0,
  onlineClients: 0,
  idleClients: 0
})

const lastUpdateTime = ref('')

const loadStatistics = async () => {
  try {
    const response = await adminApi.getStatistics()
    if (response.code === 200) {
      statistics.value = response.data || {}
      lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    }
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

const refreshStatistics = () => {
  loadStatistics()
}

const getPercentage = (value, total) => {
  if (!total || total === 0) return 0
  return Math.round((value / total) * 100)
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.statistics-page {
  padding: 1rem;
}

.statistics-grid {
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gradient-1);
}

.stat-icon {
  font-size: 3rem;
  opacity: 0.8;
  filter: drop-shadow(0 0 10px var(--glow-color));
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 0.9rem;
  color: var(--light-text);
  margin: 0 0 0.5rem 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.stat-value {
  font-size: 2.5rem;
  font-weight: bold;
  color: var(--primary-color);
  text-shadow: 0 0 20px var(--glow-color);
}

.charts-card,
.info-card {
  padding: 1.5rem;
  margin-bottom: 2rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--border-color);
}

.card-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: var(--primary-color);
  margin: 0;
  text-shadow: 0 0 10px var(--glow-color);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.refresh-button {
  padding: 0.8rem 1.5rem;
}

.charts-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.chart-section {
  padding: 1.5rem;
  background: rgba(0, 240, 255, 0.05);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
}

.chart-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--primary-color), transparent);
  animation: chartGlow 3s ease-in-out infinite;
}

@keyframes chartGlow {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.section-title {
  font-size: 1.1rem;
  font-weight: bold;
  color: var(--primary-color);
  margin: 0 0 1rem 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.chart-bar {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.bar-label {
  width: 60px;
  font-weight: bold;
  color: var(--text-color);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.bar-container {
  flex: 1;
  height: 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  margin: 0 1rem;
  overflow: hidden;
  border: 1px solid rgba(0, 240, 255, 0.2);
}

.bar-fill {
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 12px;
  position: relative;
  overflow: hidden;
}

.bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: barShine 2s ease-in-out infinite;
}

@keyframes barShine {
  0% { left: -100%; }
  100% { left: 100%; }
}

.online-bar {
  background: linear-gradient(90deg, var(--success-color), #4CAF50);
  box-shadow: 0 0 10px rgba(0, 255, 136, 0.5);
}

.offline-bar {
  background: linear-gradient(90deg, var(--error-color), #F44336);
  box-shadow: 0 0 10px rgba(255, 51, 102, 0.5);
}

.idle-bar {
  background: linear-gradient(90deg, var(--warning-color), #FFC107);
  box-shadow: 0 0 10px rgba(255, 204, 0, 0.5);
}

.busy-bar {
  background: linear-gradient(90deg, var(--primary-color), var(--secondary-color));
  box-shadow: 0 0 10px var(--glow-color);
}

.bar-value {
  width: 40px;
  text-align: right;
  font-weight: bold;
  color: var(--text-color);
  font-family: 'Courier New', monospace;
}

.info-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(0, 240, 255, 0.05);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 4px;
  transition: all 0.3s ease;
}

.info-item:hover {
  background: rgba(0, 240, 255, 0.1);
  border-color: var(--primary-color);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 240, 255, 0.2);
}

.info-label {
  font-weight: bold;
  color: var(--light-text);
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.info-value {
  color: var(--text-color);
  font-size: 1.1rem;
  font-family: 'Courier New', monospace;
}
</style>
