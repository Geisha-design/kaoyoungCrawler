<template>
  <div class="tech-container">
    <div class="register-wrapper">
      <div class="tech-card register-card">
        <h1 class="tech-title">注册新账号</h1>
        
        <div v-if="error" class="tech-alert tech-alert-error">
          {{ error }}
        </div>
        
        <div v-if="success" class="tech-alert tech-alert-success">
          {{ success }}
        </div>
        
        <form @submit.prevent="handleRegister">
          <div class="tech-form-group">
            <label class="tech-form-label">用户名</label>
            <input
              v-model="formData.username"
              type="text"
              class="tech-input"
              placeholder="请输入用户名"
              required
            />
          </div>
          
          <div class="tech-form-group">
            <label class="tech-form-label">密码</label>
            <input
              v-model="formData.password"
              type="password"
              class="tech-input"
              placeholder="请输入密码"
              required
            />
          </div>
          
          <div class="tech-form-group">
            <label class="tech-form-label">确认密码</label>
            <input
              v-model="formData.confirmPassword"
              type="password"
              class="tech-input"
              placeholder="请再次输入密码"
              required
            />
          </div>
          
          <button
            type="submit"
            class="tech-button register-button"
            :disabled="loading"
          >
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </form>
        
        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="tech-link">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api'

const router = useRouter()

const formData = ref({
  username: '',
  password: '',
  confirmPassword: '',
  clientId: 'web-client-' + Date.now()
})

const loading = ref(false)
const error = ref('')
const success = ref('')

const handleRegister = async () => {
  error.value = ''
  success.value = ''
  
  if (formData.value.password !== formData.value.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  
  if (formData.value.password.length < 6) {
    error.value = '密码长度不能少于6位'
    return
  }
  
  loading.value = true
  
  try {
    const response = await authApi.register({
      username: formData.value.username,
      password: formData.value.password,
      clientId: formData.value.clientId
    })
    
    if (response.code === 200) {
      success.value = '注册成功！正在跳转到登录页面...'
      setTimeout(() => {
        router.push('/login')
      }, 1500)
    } else {
      error.value = response.message || '注册失败，请重试'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '注册失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 2rem;
  position: relative;
  z-index: 1;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: 2.5rem;
}

.tech-title {
  text-align: center;
  font-size: 2rem;
  margin-bottom: 2rem;
  color: var(--primary-color);
  text-shadow: 0 0 20px var(--glow-color);
  letter-spacing: 2px;
  text-transform: uppercase;
}

.tech-form-group {
  margin-bottom: 1.5rem;
}

.tech-form-label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--light-text);
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.register-button {
  width: 100%;
  margin-top: 1rem;
}

.register-footer {
  text-align: center;
  margin-top: 1.5rem;
  color: var(--light-text);
}

.register-footer span {
  margin-right: 0.5rem;
}

.tech-link {
  color: var(--primary-color);
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
}

.tech-link::before {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--primary-color);
  transition: width 0.3s ease;
  box-shadow: 0 0 10px var(--glow-color);
}

.tech-link:hover {
  color: var(--primary-color);
  text-shadow: 0 0 10px var(--glow-color);
}

.tech-link:hover::before {
  width: 100%;
}
</style>
