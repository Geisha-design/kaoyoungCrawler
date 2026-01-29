<template>
  <div class="clients-page">
    <div class="ancient-card clients-card">
      <div class="card-header">
        <h3 class="card-title">客户端列表</h3>
        <button @click="refreshClients" class="ancient-button secondary refresh-button">
          刷新
        </button>
      </div>
      
      <div v-if="loading" class="ancient-loading">
        加载中...
      </div>
      
      <div v-else-if="clients.length === 0" class="ancient-empty">
        <div class="ancient-empty-icon">📭</div>
        <p>暂无客户端连接</p>
      </div>
      
      <div v-else class="table-container">
        <table class="ancient-table">
          <thead>
            <tr>
              <th>客户端ID</th>
              <th>用户名</th>
              <th>状态</th>
              <th>连接时间</th>
              <th>空闲状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="client in clients" :key="client.clientId">
              <td>{{ client.clientId }}</td>
              <td>{{ client.username }}</td>
              <td>
                <span
                  class="ancient-badge"
                  :class="client.status === 'online' ? 'success' : 'error'"
                >
                  {{ client.status === 'online' ? '在线' : '离线' }}
                </span>
              </td>
              <td>{{ formatTime(client.connectTime) }}</td>
              <td>
                <span
                  class="ancient-badge"
                  :class="client.idleStatus ? 'warning' : 'success'"
                >
                  {{ client.idleStatus ? '空闲' : '忙碌' }}
                </span>
              </td>
              <td>
                <button
                  @click="viewLogs(client.clientId)"
                  class="ancient-button secondary action-button"
                  style="padding: 6px 12px; font-size: 0.875rem;"
                >
                  查看日志
                </button>
                <button
                  v-if="client.status === 'online'"
                  @click="kickClient(client.clientId)"
                  class="ancient-button danger action-button"
                  style="padding: 6px 12px; font-size: 0.875rem;"
                >
                  踢出
                </button>
                <button
                  v-if="client.status === 'online'"
                  @click="sendHeartbeat(client.clientId)"
                  class="ancient-button action-button"
                  style="padding: 6px 12px; font-size: 0.875rem;"
                >
                  心跳
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    
    <div v-if="showLogsModal" class="modal-overlay" @click="closeLogsModal">
      <div class="modal-content ancient-card" @click.stop>
        <div class="modal-header">
          <h3>客户端日志 - {{ selectedClientId }}</h3>
          <button @click="closeLogsModal" class="close-button">×</button>
        </div>
        <div class="modal-body">
          <div v-if="logsLoading" class="ancient-loading">
            加载中...
          </div>
          <div v-else class="logs-content">
            <pre>{{ logs }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api'

const clients = ref([])
const loading = ref(false)
const showLogsModal = ref(false)
const selectedClientId = ref('')
const logs = ref('')
const logsLoading = ref(false)

const refreshClients = async () => {
  loading.value = true
  try {
    const response = await adminApi.getAllClients()
    if (response.code === 200) {
      clients.value = response.data || []
    }
  } catch (error) {
    console.error('Failed to fetch clients:', error)
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const viewLogs = async (clientId) => {
  selectedClientId.value = clientId
  showLogsModal.value = true
  logsLoading.value = true
  
  try {
    const response = await adminApi.getClientLogs(clientId)
    if (response.code === 200) {
      logs.value = JSON.stringify(response.data, null, 2)
    }
  } catch (error) {
    console.error('Failed to fetch logs:', error)
    logs.value = '加载日志失败'
  } finally {
    logsLoading.value = false
  }
}

const closeLogsModal = () => {
  showLogsModal.value = false
  logs.value = ''
}

const kickClient = async (clientId) => {
  if (!confirm(`确定要踢出客户端 ${clientId} 吗？`)) return
  
  try {
    const response = await adminApi.kickClient(clientId)
    if (response.code === 200) {
      alert('客户端已被踢出')
      refreshClients()
    }
  } catch (error) {
    console.error('Failed to kick client:', error)
    alert('踢出客户端失败')
  }
}

const sendHeartbeat = async (clientId) => {
  try {
    const response = await adminApi.sendHeartbeat(clientId)
    if (response.code === 200) {
      alert('心跳请求已发送')
    }
  } catch (error) {
    console.error('Failed to send heartbeat:', error)
    alert('发送心跳失败')
  }
}

onMounted(() => {
  refreshClients()
})
</script>

<style scoped>
.clients-page {
  padding: 1rem;
}

.clients-card {
  padding: 1.5rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.card-title {
  font-size: 1.5rem;
  font-weight: bold;
  color: var(--primary-color);
}

.refresh-button {
  padding: 8px 16px;
}

.table-container {
  overflow-x: auto;
}

.action-button {
  margin-right: 0.5rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--border-color);
}

.close-button {
  background: none;
  border: none;
  font-size: 2rem;
  color: var(--light-text);
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  color: var(--primary-color);
}

.modal-body {
  flex: 1;
  overflow-y: auto;
}

.logs-content {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 1rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  max-height: 400px;
  overflow-y: auto;
}

.logs-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
