<template>
  <div class="scripts-page">
    <div class="tech-card scripts-card">
      <div class="card-header">
        <h3 class="card-title">脚本管理</h3>
        <button @click="showCreateModal = true" class="tech-button">
          新建脚本
        </button>
      </div>
      
      <div v-if="loading" class="tech-loading">
        加载中...
      </div>
      
      <div v-else-if="scripts.length === 0" class="tech-empty">
        <div class="tech-empty-icon">📜</div>
        <p>暂无脚本</p>
      </div>
      
      <div v-else class="scripts-grid">
        <div
          v-for="script in scripts"
          :key="script.scriptId"
          class="script-card tech-card"
        >
          <div class="script-header">
            <h4 class="script-title">{{ script.description }}</h4>
            <span class="script-id">ID: {{ script.scriptId }}</span>
          </div>
          
          <div class="script-body">
            <p class="script-description">{{ script.content || '暂无描述' }}</p>
          </div>
          
          <div class="script-footer">
            <button
              @click="viewScript(script)"
              class="tech-button tech-button-secondary"
              style="padding: 6px 12px; font-size: 0.875rem;"
            >
              查看
            </button>
            <button
              @click="editScript(script)"
              class="tech-button"
              style="padding: 6px 12px; font-size: 0.875rem;"
            >
              编辑
            </button>
            <button
              @click="deleteScript(script.scriptId)"
              class="tech-button tech-button-secondary"
              style="padding: 6px 12px; font-size: 0.875rem; border-color: var(--error-color); color: var(--error-color);"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="showCreateModal || showEditModal" class="tech-modal" @click="closeModal">
      <div class="tech-modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ showCreateModal ? '新建脚本' : '编辑脚本' }}</h3>
          <button @click="closeModal" class="tech-modal-close">×</button>
        </div>
        
        <form @submit.prevent="saveScript" class="script-form">
          <div class="tech-form-group">
            <label class="tech-form-label">脚本描述</label>
            <input
              v-model="formData.description"
              type="text"
              class="tech-input"
              placeholder="请输入脚本描述"
              required
            />
          </div>
          
          <div class="tech-form-group">
            <label class="tech-form-label">脚本内容</label>
            <textarea
              v-model="formData.content"
              class="tech-input"
              placeholder="请输入脚本内容"
              rows="10"
              required
            ></textarea>
          </div>
          
          <div class="form-actions">
            <button type="button" @click="closeModal" class="tech-button tech-button-secondary">
              取消
            </button>
            <button type="submit" class="tech-button" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
    
    <div v-if="showViewModal" class="tech-modal" @click="closeViewModal">
      <div class="tech-modal-content" @click.stop>
        <div class="modal-header">
          <h3>脚本详情</h3>
          <button @click="closeViewModal" class="tech-modal-close">×</button>
        </div>
        
        <div class="script-view">
          <div class="view-item">
            <label class="view-label">脚本ID:</label>
            <span class="view-value">{{ currentScript.scriptId }}</span>
          </div>
          
          <div class="view-item">
            <label class="view-label">脚本描述:</label>
            <span class="view-value">{{ currentScript.description }}</span>
          </div>
          
          <div class="view-item">
            <label class="view-label">脚本内容:</label>
            <pre class="view-code">{{ currentScript.content }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api'

const scripts = ref([])
const loading = ref(false)
const saving = ref(false)
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showViewModal = ref(false)
const currentScript = ref({})
const formData = ref({
  description: '',
  content: ''
})

const loadScripts = async () => {
  loading.value = true
  try {
    const response = await adminApi.getAllScripts()
    if (response.code === 200) {
      scripts.value = response.data || []
    }
  } catch (error) {
    console.error('Failed to load scripts:', error)
  } finally {
    loading.value = false
  }
}

const viewScript = (script) => {
  currentScript.value = script
  showViewModal.value = true
}

const editScript = (script) => {
  currentScript.value = script
  formData.value = {
    description: script.description,
    content: script.content
  }
  showEditModal.value = true
}

const saveScript = async () => {
  saving.value = true
  
  try {
    if (showEditModal.value) {
      await adminApi.updateScript(currentScript.value.scriptId, {
        ...formData.value,
        scriptId: currentScript.value.scriptId
      })
    } else {
      await adminApi.createScript(formData.value)
    }
    
    closeModal()
    loadScripts()
  } catch (error) {
    console.error('Failed to save script:', error)
    alert('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteScript = async (scriptId) => {
  if (!confirm('确定要删除此脚本吗？')) return
  
  try {
    await adminApi.deleteScript(scriptId)
    loadScripts()
  } catch (error) {
    console.error('Failed to delete script:', error)
    alert('删除失败')
  }
}

const closeModal = () => {
  showCreateModal.value = false
  showEditModal.value = false
  formData.value = {
    description: '',
    content: ''
  }
}

const closeViewModal = () => {
  showViewModal.value = false
  currentScript.value = {}
}

onMounted(() => {
  loadScripts()
})
</script>

<style scoped>
.scripts-page {
  padding: 1rem;
}

.scripts-card {
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

.scripts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.script-card {
  padding: 1rem;
  display: flex;
  flex-direction: column;
}

.script-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-color);
}

.script-title {
  font-size: 1.1rem;
  font-weight: bold;
  color: var(--primary-color);
  margin: 0;
}

.script-id {
  font-size: 0.875rem;
  color: var(--light-text);
}

.script-body {
  flex: 1;
  margin-bottom: 1rem;
}

.script-description {
  color: var(--text-color);
  font-size: 0.9rem;
  line-height: 1.6;
  margin: 0;
}

.script-footer {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
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
  max-width: 600px;
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

.script-form {
  flex: 1;
  overflow-y: auto;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
}

.script-view {
  flex: 1;
  overflow-y: auto;
}

.view-item {
  margin-bottom: 1.5rem;
}

.view-label {
  display: block;
  font-weight: bold;
  color: var(--primary-color);
  margin-bottom: 0.5rem;
}

.view-value {
  color: var(--text-color);
}

.view-code {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 1rem;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  max-height: 400px;
  overflow-y: auto;
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
