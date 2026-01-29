<template>
  <div class="ancient-container">
    <div class="login-wrapper">
      <div class="ancient-card login-card">
        <h1 class="ancient-title">考优爬虫管理系统</h1>
        
        <div v-if="error" class="ancient-alert error">
          {{ error }}
        </div>
        
        <form @submit.prevent="handleLogin">
          <div class="ancient-form-group">
            <label class="ancient-form-label">用户名</label>
            <input
              v-model="formData.username"
              type="text"
              class="ancient-input"
              placeholder="请输入用户名"
              required
            />
          </div>
          
          <div class="ancient-form-group">
            <label class="ancient-form-label">密码</label>
            <input
              v-model="formData.password"
              type="password"
              class="ancient-input"
              placeholder="请输入密码"
              required
            />
          </div>
          
          <button
            type="submit"
            class="ancient-button login-button"
            :disabled="loading"
          >
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
        
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="ancient-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const formData = ref({
  username: '',
  password: '',
  clientId: 'web-client-' + Date.now()
})

const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  error.value = ''
  loading.value = true
  
  try {
    const response = await authApi.login(formData.value)
    
    if (response.code === 200) {
      userStore.setToken(response.data.token)
      userStore.setUsername(formData.value.username)
      router.push('/')
    } else {
      error.value = response.message || '登录失败，请重试'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '登录失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 2rem;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 2.5rem;
}

.login-button {
  width: 100%;
  margin-top: 1rem;
}

.login-footer {
  text-align: center;
  margin-top: 1.5rem;
  color: var(--light-text);
}

.login-footer span {
  margin-right: 0.5rem;
}
</style>
