# 考优爬虫管理系统 - 前端

基于Vue3的古风风格爬虫管理系统前端，提供完整的登录注册、客户端管理、脚本管理和统计分析功能。

## 技术栈

- Vue 3 (Composition API)
- Vue Router 4
- Pinia (状态管理)
- Axios (HTTP请求)
- Vite (构建工具)

## 项目结构

```
kaoyoung-crawler-admin/
├── src/
│   ├── api/              # API接口封装
│   │   └── index.js
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── stores/           # Pinia状态管理
│   │   └── user.js
│   ├── styles/           # 全局样式
│   │   └── ancient.css   # 古风样式
│   ├── views/            # 页面组件
│   │   ├── Login.vue     # 登录页
│   │   ├── Register.vue  # 注册页
│   │   ├── Dashboard.vue # 主布局
│   │   ├── Clients.vue   # 客户端管理
│   │   ├── Scripts.vue   # 脚本管理
│   │   └── Statistics.vue# 统计分析
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── public/               # 静态资源
├── index.html
├── package.json
└── vite.config.js
```

## 功能特性

### 1. 用户认证
- 用户登录
- 用户注册
- 用户登出
- JWT Token管理

### 2. 客户端管理
- 查看所有客户端状态
- 查看客户端详细信息
- 查看客户端日志
- 踢出客户端
- 发送心跳检测

### 3. 脚本管理
- 查看所有脚本
- 创建新脚本
- 编辑脚本
- 删除脚本
- 查看脚本详情

### 4. 统计分析
- 客户端总数统计
- 在线/离线客户端统计
- 空闲/忙碌客户端统计
- 可视化图表展示

## 古风设计特点

### 色彩方案
- 主色调：棕色系 (#8B4513, #D2691E, #CD853F)
- 背景色：米色系 (#F5E6D3, #FFF8DC)
- 文字色：深棕色 (#2F1810, #6B4423)

### 设计元素
- 古典卡片样式，带双层边框
- 渐变按钮设计
- 优雅的表格样式
- 徽章标签设计
- 响应式布局

### 字体
- 优先使用楷体 (KaiTi, STKaiti)
- 备用微软雅黑

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## API配置

API基础地址配置在 `src/api/index.js` 中：

```javascript
const API_BASE_URL = 'http://localhost:8090/smartebaoCrawler/api'
```

如需修改后端地址，请更新此配置。

## 使用说明

### 登录系统

1. 访问系统首页，自动跳转到登录页面
2. 输入用户名和密码
3. 点击"登录"按钮
4. 登录成功后进入管理后台

### 注册账号

1. 在登录页面点击"立即注册"
2. 填写用户名、密码和确认密码
3. 点击"注册"按钮
4. 注册成功后跳转到登录页面

### 客户端管理

- 查看所有连接的客户端及其状态
- 点击"查看日志"查看客户端日志
- 点击"踢出"强制客户端下线
- 点击"心跳"发送心跳检测请求

### 脚本管理

- 点击"新建脚本"创建新的爬虫脚本
- 点击"查看"查看脚本详细信息
- 点击"编辑"修改脚本内容
- 点击"删除"删除脚本

### 统计分析

- 查看系统整体统计数据
- 查看客户端状态分布图表
- 查看客户端工作状态图表

## 注意事项

1. 确保后端服务正常运行（端口8090）
2. 首次使用需要先注册账号
3. Token有效期为2小时，过期后需要重新登录
4. 建议使用现代浏览器（Chrome、Firefox、Edge等）

## 开发说明

### 添加新页面

1. 在 `src/views/` 目录下创建新的Vue组件
2. 在 `src/router/index.js` 中添加路由配置
3. 在侧边栏添加导航链接（如需要）

### 添加新API

1. 在 `src/api/index.js` 中添加API方法
2. 在组件中导入并使用

### 修改样式

全局样式在 `src/styles/ancient.css` 中修改
页面特定样式在组件的 `<style scoped>` 中修改

## 浏览器兼容性

- Chrome (推荐)
- Firefox
- Edge
- Safari

## 许可证

MIT License
