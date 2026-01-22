# API 接口示例请求

## 1. 用户登录接口

### 请求信息
- **URL**: `http://localhost:8090/smarteCrawler/api/login`
- **Method**: POST
- **Content-Type**: application/json

### 请求参数
```json
{
  "username": "testuser",
  "password": "testpass",
  "clientId": "extension-id-here"
}
```

### 响应示例
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

---

## 2. 用户注册接口

### 请求信息
- **URL**: `http://localhost:8090/smarteCrawler/api/register`
- **Method**: POST
- **Content-Type**: application/json

### 请求参数
```json
{
  "username": "newuser",
  "password": "newpass",
  "clientId": "extension-id-here"
}
```

### 响应示例
```json
{
  "code": 200,
  "message": "注册成功"
}
```

---

## 3. WebSocket 连接

### WebSocket 地址
```
ws://localhost:8090/smarteCrawler/ws?token=eyJhbGciOiJIUzI1NiJ9...
```

### 认证成功响应
```json
{
  "type": "auth_success",
  "payload": {
    "clientId": "extension-id-here"
  },
  "clientId": "extension-id-here",
  "timestamp": 1678886400000
}
```

### 客户端注册消息
```json
{
  "type": "register",
  "payload": {
    "username": "testuser",
    "currentUrl": "https://example.com",
    "supportTaskTypes": "product_crawl,article_crawl,idle_task",
    "idleStatus": false
  },
  "clientId": "extension-id-here",
  "timestamp": 1678886400000
}
```

### 爬取结果上报
```json
{
  "type": "crawl_result",
  "payload": {
    "taskId": "task-123",
    "crawlData": {
      "title": "Example Title",
      "content": "Example Content"
    },
    "crawlStatus": "success"
  },
  "clientId": "extension-id-here",
  "timestamp": 1678886400000
}
```

### 空闲状态更新
```json
{
  "type": "idle_status_update",
  "payload": {
    "isIdle": true,
    "timestamp": 1678886400000,
    "idleDuration": 300000
  },
  "clientId": "extension-id-here",
  "timestamp": 1678886400000
}
```

### 心跳消息
```json
{
  "type": "heartbeat",
  "payload": {
    "timestamp": 1678886400000
  },
  "clientId": "extension-id-here",
  "timestamp": 1678886400000
}
```

---

## 4. 数据库表结构

### crawler_user 表
```sql
CREATE TABLE crawler_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### crawler_client 表
```sql
CREATE TABLE crawler_client (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  client_id VARCHAR(100) NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL,
  current_url TEXT,
  support_task_types VARCHAR(255),
  browser VARCHAR(100),
  connect_time DATETIME,
  status VARCHAR(20) DEFAULT 'offline',
  last_update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  idle_status BOOLEAN DEFAULT FALSE
);
```

### crawler_result 表
```sql
CREATE TABLE crawler_result (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id VARCHAR(100),
  client_id VARCHAR(100),
  result_data JSON,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 5. 浏览器扩展功能

### 发送用户进入空闲状态
```javascript
chrome.runtime.sendMessage({
  type: 'user_idle',
  timestamp: Date.now()
});
```

### 发送用户活跃状态
```javascript
chrome.runtime.sendMessage({
  type: 'user_active',
  timestamp: Date.now()
});
```

### 发送爬取结果
```javascript
chrome.runtime.sendMessage({
  type: 'crawl_result_from_content',
  taskId: 'task-123',
  result: { title: 'Example', content: 'Content' },
  status: 'success'
});
```