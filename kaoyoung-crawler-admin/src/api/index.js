import axios from 'axios'

const API_BASE_URL = 'http://localhost:8090/smarteCrawler/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          localStorage.removeItem('token')
          window.location.href = '/login'
          break
        case 403:
          console.error('没有权限访问')
          break
        case 404:
          console.error('请求的资源不存在')
          break
        case 500:
          console.error('服务器错误')
          break
      }
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => api.post('/login', data),
  register: (data) => api.post('/register', data),
  logout: (data) => api.post('/logout', data)
}

export const adminApi = {
  getAllClients: () => api.get('/admin/clients'),
  kickClient: (clientId) => api.post(`/admin/clients/${clientId}/kick`),
  sendHeartbeat: (clientId) => api.post(`/admin/clients/${clientId}/heartbeat`),
  getClientLogs: (clientId) => api.get(`/admin/clients/${clientId}/logs`),
  getStatistics: () => api.get('/admin/statistics'),
  getAllScripts: () => api.get('/admin/scripts'),
  createScript: (script) => api.post('/admin/scripts', script),
  updateScript: (scriptId, script) => api.put(`/admin/scripts/${scriptId}`, script),
  deleteScript: (scriptId) => api.delete(`/admin/scripts/${scriptId}`),
  executeScript: (clientId, params) => api.post(`/admin/clients/${clientId}/execute-script`, params),
  executeScriptById: (clientId, params) => api.post(`/admin/clients/${clientId}/execute-script-by-id`, params)
}

export default api
