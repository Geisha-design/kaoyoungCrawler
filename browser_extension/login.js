// 定义全局函数，以便在任何地方都可以调用
window.getClientId = async function() {
  return new Promise((resolve, reject) => {
    chrome.runtime.sendMessage({ type: 'get_client_id' }, function(response) {
      if (chrome.runtime.lastError) {
        reject(chrome.runtime.lastError);
      } else {
        resolve(response);
      }
    });
  });
};

// 登录逻辑
document.addEventListener('DOMContentLoaded', function() {
  // 获取DOM元素
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const connectedPage = document.getElementById('connectedPage');
  const loginBtn = document.getElementById('loginBtn');
  const registerBtn = document.getElementById('registerBtn');
  const showRegisterBtn = document.getElementById('showRegisterBtn');
  const showLoginBtn = document.getElementById('showLoginBtn');
  const statusDiv = document.getElementById('status');
  const regStatusDiv = document.getElementById('regStatus');
  const backBtn = document.getElementById('backBtn');
  
  // 获取显示元素
  const clientIdDisplay = document.getElementById('clientIdDisplay');
  const usernameDisplay = document.getElementById('usernameDisplay');
  const currentUrlDisplay = document.getElementById('currentUrlDisplay');
  
  // 检查当前连接状态
  checkCurrentStatus();
  
  // 登录按钮点击事件
  loginBtn.addEventListener('click', async function() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    if (!username || !password) {
      statusDiv.textContent = '请输入用户名和密码';
      statusDiv.className = 'disconnected';
      return;
    }
    
    try {
      // 获取浏览器指纹生成的客户端ID
      const clientInfo = await window.getClientId();
      const extensionId = clientInfo.clientId;
      
      // 调用后端登录接口
      const response = await fetch('http://localhost:8090/smarteCrawler/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: username,
          password: password,
          clientId: extensionId  // 发送浏览器指纹生成的客户端ID
        })
      });
      
      const result = await response.json();
      
      if (result.code === 200) {
        // 保存JWT令牌到Chrome存储
        chrome.storage.local.set({
          jwtToken: result.data.token,
          username: username
          // 客户端ID已经存储在background中
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
  
  // 注册按钮点击事件
  registerBtn.addEventListener('click', async function() {
    const username = document.getElementById('regUsername').value;
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;
    
    if (!username || !password || !confirmPassword) {
      regStatusDiv.textContent = '请填写所有字段';
      regStatusDiv.className = 'disconnected';
      return;
    }
    
    if (password !== confirmPassword) {
      regStatusDiv.textContent = '两次输入的密码不一致';
      regStatusDiv.className = 'disconnected';
      return;
    }
    
    if (password.length < 6) {
      regStatusDiv.textContent = '密码长度至少为6位';
      regStatusDiv.className = 'disconnected';
      return;
    }
    
    try {
      // 获取浏览器指纹生成的客户端ID
      const clientInfo = await window.getClientId();
      const extensionId = clientInfo.clientId;
      
      // 调用后端注册接口
      const response = await fetch('http://localhost:8090/smarteCrawler/api/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: username,
          password: password,
          clientId: extensionId  // 发送浏览器指纹生成的客户端ID
        })
      });
      
      const result = await response.json();
      
      if (result.code === 200) {
        regStatusDiv.textContent = '注册成功，请登录';
        regStatusDiv.className = 'connected';
        
        // 清空注册表单
        document.getElementById('regUsername').value = '';
        document.getElementById('regPassword').value = '';
        document.getElementById('regConfirmPassword').value = '';
        
        // 自动切换到登录页面
        setTimeout(() => {
          registerForm.style.display = 'none';
          loginForm.style.display = 'block';
          regStatusDiv.textContent = '';
        }, 1500);
      } else {
        regStatusDiv.textContent = result.message || '注册失败';
        regStatusDiv.className = 'disconnected';
      }
    } catch (error) {
      console.error('注册错误:', error);
      regStatusDiv.textContent = '注册失败: ' + error.message;
      regStatusDiv.className = 'disconnected';
    }
  });
  
  // 显示注册页面
  showRegisterBtn.addEventListener('click', function() {
    loginForm.style.display = 'none';
    registerForm.style.display = 'block';
  });
  
  // 显示登录页面
  showLoginBtn.addEventListener('click', function() {
    registerForm.style.display = 'none';
    loginForm.style.display = 'block';
    regStatusDiv.textContent = '';
  });
  
  // 监听返回按钮点击
  backBtn.addEventListener('click', function() {
    connectedPage.style.display = 'none';
    loginForm.style.display = 'block';
  });
  
  // 监听复制客户端ID按钮点击
  const copyClientIdBtn = document.getElementById('copyClientIdBtn');
  copyClientIdBtn.addEventListener('click', async function() {
    try {
      const response = await window.getClientId();
      if (response && response.clientId) {
        // 复制客户端ID到剪贴板
        await navigator.clipboard.writeText(response.clientId);
        statusDiv.textContent = '客户端ID已复制到剪贴板';
        statusDiv.className = 'connected';
        
        // 3秒后清除状态消息
        setTimeout(() => {
          statusDiv.textContent = '';
        }, 3000);
      } else {
        statusDiv.textContent = '无法获取客户端ID';
        statusDiv.className = 'disconnected';
      }
    } catch (error) {
      console.error('获取客户端ID失败:', error);
      statusDiv.textContent = '获取客户端ID失败: ' + error.message;
      statusDiv.className = 'disconnected';
    }
  });
  
  // 监听退出登录按钮点击
  const logoutBtn = document.getElementById('logoutBtn');
  logoutBtn.addEventListener('click', async function() {
    try {
      // 获取客户端ID
      const clientInfo = await window.getClientId();
      const extensionId = clientInfo.clientId;
      
      // 获取JWT令牌
      const storedData = await chrome.storage.local.get(['jwtToken']);
      
      // 调用后端退出登录接口
      if (storedData.jwtToken) {
        try {
          const response = await fetch('http://localhost:8090/smarteCrawler/api/logout', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${storedData.jwtToken}`
            },
            body: JSON.stringify({
              clientId: extensionId
            })
          });
          
          const result = await response.json();
          console.log('后端退出登录结果:', result);
        } catch (error) {
          console.error('调用后端退出登录接口失败:', error);
        }
      }
      
      // 通知background脚本关闭WebSocket连接
      chrome.runtime.sendMessage({ type: 'logout' }, function(response) {
        if (chrome.runtime.lastError) {
          console.error('发送退出消息失败:', chrome.runtime.lastError);
        }
        
        // 清除本地存储的JWT令牌，但保留clientId因为它代表客户端的唯一标识
        chrome.storage.local.remove(['jwtToken', 'username'], function() {
          // 重置界面状态
          loginForm.style.display = 'block';
          connectedPage.style.display = 'none';
          
          // 清空输入框
          document.getElementById('username').value = '';
          document.getElementById('password').value = '';
          
          statusDiv.textContent = '已退出登录';
          statusDiv.className = 'disconnected';
          
          // 3秒后清除状态消息
          setTimeout(() => {
            statusDiv.textContent = '';
          }, 3000);
        });
      });
    } catch (error) {
      console.error('退出登录失败:', error);
      statusDiv.textContent = '退出登录失败: ' + error.message;
      statusDiv.className = 'disconnected';
    }
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
    // 从本地存储获取用户名和当前URL
    chrome.storage.local.get(['username', 'currentUrl'], function(result) {
      // 更新显示信息
      clientIdDisplay.textContent = status.clientId || 'N/A';
      usernameDisplay.textContent = result.username || status.username || '-';
      currentUrlDisplay.textContent = result.currentUrl ? 
        new URL(result.currentUrl).hostname : (status.activeTabUrl ? 
        new URL(status.activeTabUrl).hostname : '-');
      
      // 切换页面显示
      loginForm.style.display = 'none';
      connectedPage.style.display = 'block';
      
      statusDiv.textContent = '连接成功！';
      statusDiv.className = 'connected';
    });
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