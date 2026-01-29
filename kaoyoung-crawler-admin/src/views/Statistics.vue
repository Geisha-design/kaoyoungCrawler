<template>
  <div class="statistics-page">
    <div class="statistics-grid">
      <div class="stat-card ancient-card">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <h4 class="stat-title">总客户端数</h4>
          <div class="stat-value">{{ statistics.totalClients || 0 }}</div>
        </div>
      </div>
      
      <div class="stat-card ancient-card">
        <div class="stat-icon">🟢</div>
        <div class="stat-content">
          <h4 class="stat-title">在线客户端</h4>
          <div class="stat-value">{{ statistics.onlineClients || 0 }}</div>
        </div>
      </div>
      
      <div class="stat-card ancient-card">
        <div class="stat-icon">🟡</div>
        <div class="stat-content">
          <h4 class="stat-title">空闲客户端</h4>
          <div class="stat-value">{{ statistics.idleClients || 0 }}</div>
        </div>
      </div>
      
      <div class="stat-card ancient-card">
        <div class="stat-icon">📈</div>
        <div class="stat-content">
          <h4 class="stat-title">忙碌客户端</h4>
          <div class="stat-value">{{ (statistics.onlineClients || 0) - (statistics.idleClients || 0) }}</div>
        </div>
      </div>
    </div>
    
    <div class="ancient-card charts-card">
      <div class="card-header">
        <h3 class="card-title">系统概览</h3>
        <button @click="refreshStatistics" class="ancient-button secondary refresh-button">
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
                class="bar-fill online-bar"
                :style="{ width: getPercentage(statistics.onlineClients, statistics.totalClients) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ statistics.onlineClients || 0 }}</div>
          </div>
          
          <div class="chart-bar">
            <div class="bar-label">离线</div>
            <div class="bar-container">
              <div
                class="bar-fill offline-bar"
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
                class="bar-fill idle-bar"
                :style="{ width: getPercentage(statistics.idleClients, statistics.onlineClients) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ statistics.idleClients || 0 }}</div>
          </div>
          
          <div class="chart-bar">
            <div class="bar-label">忙碌</div>
            <div class="bar-container">
              <div
                class="bar-fill busy-bar"
                :style="{ width: getPercentage(statistics.onlineClients - statistics.idleClients, statistics.onlineClients) + '%' }"
              ></div>
            </div>
            <div class="bar-value">{{ (statistics.onlineClients || 0) - (statistics.idleClients || 0) }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="ancient-card info-card">
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
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  font-size: 3rem;
  opacity: 0.8;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 0.9rem;
  color: var(--light-text);
  margin: 0 0 0.5rem 0;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: var(--primary-color);
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
}

.refresh-button {
  padding: 8px 16px;
}

.charts-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.chart-section {
  padding: 1rem;
  background: rgba(139, 69, 19, 0.05);
  border-radius: 4px;
}

.section-title {
  font-size: 1.1rem;
  font-weight: bold;
  color: var(--primary-color);
  margin: 0 0 1rem 0;
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
}

.bar-container {
  flex: 1;
  height: 24px;
  background: #e0e0e0;
  border-radius: 12px;
  margin: 0 1rem;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 12px;
}

.online-bar {
  background: linear-gradient(90deg, var(--success-color), #4CAF50);
}

.offline-bar {
  background: linear-gradient(90deg, var(--error-color), #F44336);
}

.idle-bar {
  background: linear-gradient(90deg, var(--warning-color), #FFC107);
}

.busy-bar {
  background: linear-gradient(90deg, var(--primary-color), var(--secondary-color));
}

.bar-value {
  width: 40px;
  text-align: right;
  font-weight: bold;
  color: var(--text-color);
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
}

.info-label {
  font-weight: bold;
  color: var(--light-text);
  font-size: 0.9rem;
}

.info-value {
  color: var(--text-color);
  font-size: 1.1rem;
}
</style>
