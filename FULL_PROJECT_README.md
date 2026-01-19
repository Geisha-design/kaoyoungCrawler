# 爬虫助手系统

一个分布式爬虫助手系统，包含Spring Boot后端服务和Chrome浏览器扩展客户端，通过WebSocket实现双向实时通信。

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      浏览器插件客户端                        │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐ │
│  │  登录模块  │  │ WebSocket │  │ 脚本缓存  │  │ 爬取执行  │ │
│  │（JWT获取） │  │  通信模块  │  │（增量更新）│  │（DOM操作）│ │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘ │
│  ┌───────────┐  ┌───────────────────────────────────────┐   │
│  │域名脚本映射│  │         定时任务管理模块             │   │
│  │（自动调度）│  │（可视化操作、alarms定时、状态同步）  │   │
│  └───────────┘  └───────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │ WebSocket双向通信
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Spring Boot 后端                        │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐ │
│  │ 认证模块  │  │ WebSocket │  │ 脚本管理  │  │ 任务管理  │ │
│  │（JWT验证） │  │  处理器   │  │（批量/定向）│  │（下发/记录）│ │
│  └───────────┘  └───────────┘  └───────────┘  └───────────┘ │
│  ┌───────────┐  ┌───────────────────────────────────────┐   │
│  │ 数据访问  │  │ 日志模块（脚本下发/结果/定时任务日志）│   │
│  │（MyBatis-Plus）│                                   │   │
│  └───────────┘  └───────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │ JDBC数据库连接
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                        MySQL 数据库                          │
│（用户表、客户端表、脚本表、任务表、结果表、下发日志表、定时任务表）│
└─────────────────────────────────────────────────────────────┘
```

## 项目组成

### 后端服务 (Spring Boot)
位于根目录，提供：
- JWT身份认证服务
- WebSocket实时通信
- 脚本批量/定向下发
- 任务管理与调度
- 数据持久化

### 浏览器插件 (Chrome Extension)
位于 `browser_extension/` 目录，提供：
- 用户登录界面
- WebSocket连接管理
- 网页爬取执行
- 定时任务管理

## 功能特性

### 基础功能
- ✅ JWT身份认证
- ✅ WebSocket双向实时通信
- ✅ 脚本批量下发与定向下发
- ✅ 网址监测与同步
- ✅ 爬取结果上传

### 高级功能
- ✅ 网站爬取调度（基于域名匹配）
- ✅ 定时任务管理
- ✅ 可视化任务配置界面
- ✅ 数据持久化至MySQL
- ✅ 完整的日志记录

## 快速开始

### 1. 后端服务部署

#### 环境准备
- JDK 8或11
- Maven 3.6+
- MySQL 8.0+

#### 数据库配置
```sql
CREATE DATABASE crawler_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

执行 `src/main/resources/schema.sql` 初始化表结构。

#### 应用配置
修改 `src/main/resources/application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/crawler_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: root  # 替换为实际用户名
      password: root  # 替换为实际密码
```

#### 启动后端服务
```bash
mvn spring-boot:run
```

### 2. 浏览器插件安装

1. 打开Chrome浏览器，访问 `chrome://extensions/`
2. 开启"开发者模式"
3. 点击"加载已解压的扩展程序"
4. 选择 `browser_extension` 目录

### 3. 系统使用

1. 点击浏览器工具栏中的爬虫助手图标
2. 输入用户名密码（默认 admin/123456）
3. 插件自动连接后端服务并接收脚本
4. 访问目标网站时自动执行匹配的爬取脚本

## API接口

### HTTP接口
- `POST /api/login` - 用户登录
- `POST /api/script/designated/push` - 脚本定向下发
- `GET /api/scheduled-task/list/{clientId}` - 获取定时任务列表
- `POST /api/scheduled-task/sync` - 同步定时任务配置

### WebSocket接口
- `ws://localhost:8080/ws?token=xxx` - WebSocket连接地址

## 技术栈

### 后端
- Spring Boot 2.7.x
- Spring WebSocket
- MyBatis-Plus
- MySQL
- JWT
- FastJSON
- Lombok
- Druid连接池

### 前端（浏览器插件）
- Chrome Extension Manifest V3
- JavaScript
- Chrome APIs

## 数据库表结构

- `crawler_user`: 用户表
- `crawler_client`: 客户端注册表
- `crawler_script`: 爬取脚本表
- `crawler_task`: 爬取任务表
- `crawler_result`: 爬取结果表
- `crawler_script_push_log`: 脚本下发日志表
- `crawler_scheduled_task`: 定时任务表

## 安全特性

- JWT令牌认证，防止未授权访问
- WebSocket连接验证
- 脚本执行沙箱化
- 敏感数据加密存储

## 扩展性设计

- 模块化架构，易于功能扩展
- 支持多客户端并发连接
- 可扩展至其他浏览器（Firefox、Edge）
- 微服务架构支持水平扩展

## 故障排除

常见问题及解决方案：

1. **WebSocket连接失败**
   - 检查后端服务是否运行
   - 验证JWT令牌是否有效
   - 检查跨域配置

2. **脚本下发失败**
   - 确认客户端在线状态
   - 验证脚本ID是否存在
   - 检查WebSocket会话状态

3. **数据库连接失败**
   - 验证数据库配置
   - 检查数据库服务状态
   - 确认数据库用户权限

## 许可证

MIT License