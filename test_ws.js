// 测试WebSocket连接
const WebSocket = require('ws');

// 使用从登录获取的token
const token = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc2OTA2MTA4MiwiZXhwIjoxNzY5MDY4MjgyfQ.nmRrEJGzmLhitTJ17E7jAUIcLEWQFV8a2leMEVPtuWD0Co1tNMgrqunBP8o_EU25_IBOcgatPgMzC53moGhDKQ';

console.log('尝试连接WebSocket...');

const ws = new WebSocket('ws://localhost:8080/smarteCrawler/ws?token=' + encodeURIComponent(token));

ws.on('open', function open() {
  console.log('✅ WebSocket连接已成功建立');
  
  // 发送一个注册消息
  const registerMsg = {
    type: 'register',
    clientId: 'test_client_' + Date.now(),
    payload: {
      username: 'admin',
      currentUrl: 'http://localhost:8080/test',
      supportTaskTypes: 'test_task',
      idleStatus: false
    },
    timestamp: Date.now()
  };
  
  console.log('📤 发送注册消息:', JSON.stringify(registerMsg));
  ws.send(JSON.stringify(registerMsg));
});

ws.on('message', function message(data) {
  console.log('📥 收到消息:', data.toString());
});

ws.on('close', function close(code, reason) {
  console.log('❌ WebSocket连接已关闭:', code, reason ? reason : '(无原因)');
});

ws.on('error', function error(err) {
  console.error('🚨 WebSocket错误:', err.message);
});