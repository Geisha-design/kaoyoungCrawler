// 登录逻辑
document.addEventListener('DOMContentLoaded', function() {
  // 获取DOM元素
  const loginForm = document.getElementById('loginForm');
  const connectedPage = document.getElementById('connectedPage');
  const loginBtn = document.getElementById('loginBtn');
  const statusDiv = document.getElementById('status');
  const backBtn = document.getElementById('backBtn');
  
  // 获取显示元素
  const clientIdDisplay = document.getElementById('clientIdDisplay');
  const usernameDisplay = document.getElementById('usernameDisplay');
  const currentUrlDisplay = document.getElementById('currentUrlDisplay');
  
  // 检查当前连接状态
  checkCurrentStatus();
  
  loginBtn.addEventListener('click', async function() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    if (!username || !password) {
      statusDiv.textContent = '请输入用户名和密码';
      statusDiv.className = 'disconnected';
      return;
    }
    
    try {
      // 调用后端登录接口
      const response = await fetch('http://localhost:8090/smarteCrawler/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: username,
          password: password
        })
      });
      
      const result = await response.json();
      
      if (result.code === 200) {
        // 保存JWT令牌到Chrome存储
        chrome.storage.local.set({
          jwtToken: result.data.token,
          username: username
        }, function() {
          statusDiv.textContent = '登录成功，正在连接WebSocket...';
          statusDiv.className = 'connected';
          
          // 通知background脚本开始WebSocket连接
          chrome.runtime.sendMessage({ type: 'login_success', token: result.data.token, username: username });
          
          // 监听连接状态更新
          setTimeout(checkWebSocketConnection, 1000);
        });
      } else {
        statusDiv.textContent = result.message || '登录失败';
        statusDiv.className = 'disconnected';
      }
    } catch (error) {
      console.error('登录错误:', error);
      statusDiv.textContent = '登录失败: ' + error.message;
      statusDiv.className = 'disconnected';
    }
  });
  
  // 监听返回按钮点击
  backBtn.addEventListener('click', function() {
    connectedPage.style.display = 'none';
    loginForm.style.display = 'block';
  });
  
  // 检查WebSocket连接状态
  async function checkWebSocketConnection() {
    chrome.runtime.sendMessage({ type: 'get_status' }, function(response) {
      if (response && response.isConnected) {
        // 显示连接成功页面
        showConnectedPage(response);
      } else {
        // 如果未连接，稍后再次检查
        setTimeout(checkWebSocketConnection, 1000);
      }
    });
  }
  
  // 显示连接成功页面
  function showConnectedPage(status) {
    // 更新显示信息
    clientIdDisplay.textContent = status.clientId || '-';
    usernameDisplay.textContent = status.username || '-';
    currentUrlDisplay.textContent = status.activeTabUrl ? 
      new URL(status.activeTabUrl).hostname : '-';
    
    // 切换页面显示
    loginForm.style.display = 'none';
    connectedPage.style.display = 'block';
    
    statusDiv.textContent = '连接成功！';
    statusDiv.className = 'connected';
  }
  
  // 检查当前状态
  async function checkCurrentStatus() {
    // 尝试获取存储的JWT令牌
    chrome.storage.local.get(['jwtToken', 'username'], function(result) {
      if (result.jwtToken) {
        // 如果有令牌，检查WebSocket连接状态
        chrome.runtime.sendMessage({ type: 'get_status' }, function(response) {
          if (response && response.isConnected) {
            // 如果已连接，直接显示连接成功页面
            document.getElementById('username').value = result.username || '';
            showConnectedPage(response);
          } else {
            // 如果未连接，显示登录表单
            loginForm.style.display = 'block';
            connectedPage.style.display = 'none';
          }
        });
      } else {
        // 如果没有令牌，显示登录表单
        loginForm.style.display = 'block';
        connectedPage.style.display = 'none';
      }
    });
  }
});