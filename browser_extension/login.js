// 登录逻辑
document.addEventListener('DOMContentLoaded', function() {
  const loginBtn = document.getElementById('loginBtn');
  const statusDiv = document.getElementById('status');
  
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
      const response = await fetch('http://localhost:8080/smarteCrawler/api/login', {
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
});