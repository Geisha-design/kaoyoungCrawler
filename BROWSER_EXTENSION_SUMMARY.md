# 爬虫助手浏览器插件 - 项目总结

## 项目概述

我们成功构建了一个Chrome浏览器扩展，作为爬虫助手系统的客户端。该插件与后端服务通过WebSocket进行实时通信，实现了JS爬取脚本的批量下发、定向下发、网站爬取调度和定时任务管理等功能。

## 核心功能

### 1. JWT身份认证
- 通过popup.html提供登录界面
- login.js调用后端登录接口获取JWT令牌
- 令牌缓存至Chrome本地存储，供WebSocket连接使用

### 2. WebSocket实时通信
- background.js初始化WebSocket连接，携带JWT令牌
- 实现断线重连逻辑（5秒重试）
- 处理服务端消息（认证结果、脚本下发、任务指令）

### 3. 网址监测与同步
- 通过Chrome API（chrome.tabs.onUpdated）监听标签页网址变化
- 实时同步当前访问页面URL至服务端
- 确保服务端掌握客户端当前状态

### 4. 爬取脚本执行
- content.js负责在页面上下文中执行爬取脚本
- 接收服务端任务指令，从缓存读取对应脚本
- 执行页面爬取并将结果回传至background.js

### 5. 网站爬取调度
- 基于后端下发脚本的domainPattern字段构建域名-脚本映射表
- 监听标签页更新和切换事件，自动匹配域名与脚本
- 支持自动执行或弹窗提示用户执行

### 6. 定时任务管理
- 基于Chrome Alarms API实现定时任务调度
- 支持任务的可视化管理（添加、更新、删除、启用/禁用）
- 任务配置持久化并与后端同步
- 仅在目标域名页面执行对应爬取任务

### 7. 结果上传与确认
- 将爬取结果上传至服务端进行存储
- 支持即时任务和定时任务的结果上传

## 技术架构

### 插件技术栈
- **Chrome Extension Manifest V3**: 现代化扩展架构
- **WebSocket**: 实时双向通信
- **Chrome Storage API**: 本地数据持久化
- **Chrome Tabs API**: 标签页状态监控
- **Chrome Alarms API**: 定时任务调度
- **Chrome Scripting API**: 页面脚本注入

### 文件结构
```
browser_extension/
├── manifest.json              # 扩展配置
├── popup.html               # 登录弹窗界面
├── login.js                 # 登录逻辑
├── background.js            # 后台服务脚本（核心逻辑）
├── content.js               # 内容脚本（页面执行环境）
├── task-manager.html        # 定时任务管理界面
├── task-manager.js          # 定时任务管理逻辑
├── icons/                   # 图标资源
└── README.md                # 使用说明
```

## 消息通信机制

### WebSocket消息格式
所有消息采用统一JSON格式：
```json
{
  "type": "消息类型",
  "payload": "消息体",
  "clientId": "客户端唯一标识",
  "timestamp": "时间戳"
}
```

### 核心消息类型
- `auth_success`: 连接认证成功，返回clientId
- `script_push`: 批量下发脚本（含domainPattern）
- `script_designated_push`: 定向下发脚本
- `task_command`: 下发爬取任务指令
- `crawl_result`: 上传爬取结果
- `scheduled_task_config`: 同步定时任务配置

## 部署与使用

### 开发者模式安装
1. 打开Chrome `chrome://extensions/`
2. 开启“开发者模式”
3. 点击“加载已解压的扩展程序”
4. 选择browser_extension目录

### 使用流程
1. 点击插件图标打开登录界面
2. 输入用户名密码登录（如admin/123456）
3. 插件自动连接到后端WebSocket服务
4. 接收并缓存爬取脚本
5. 访问匹配网站时自动提示或执行脚本

## 安全特性

- JWT令牌认证，有效期内自动续期
- WebSocket连接验证，防止未授权访问
- 脚本执行沙箱化，限制危险操作
- 通信数据加密传输

## 扩展性设计

- 模块化架构，便于功能扩展
- 支持多浏览器适配（未来可扩展Firefox、Edge）
- 定时任务支持复杂调度策略
- 插件休眠保护，避免影响定时任务执行

## 与其他系统的集成

浏览器插件与后端Spring Boot服务紧密协作，共同构成完整的分布式爬虫助手系统，实现"指令下发-脚本执行-数据回传-存储确认"的全流程闭环。