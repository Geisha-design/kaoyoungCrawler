# 爬虫助手系统 - 客户端脚本执行功能说明文档

## 概述

本文档详细说明了爬虫助手系统新增的客户端脚本执行功能，允许管理员指定特定客户端执行脚本库中的脚本。

## 功能特性

### 1. 指定客户端执行脚本
- 支持指定特定客户端ID执行脚本库中的脚本
- 支持两种API调用方式：
  - POST `/api/client-script/execute` (请求体传递参数)
  - POST `/api/client-script/clients/{clientId}/scripts/{scriptId}/execute` (路径参数传递)

### 2. 脚本库管理
- 支持从脚本库中选择脚本执行
- 自动验证脚本存在性

### 3. 客户端状态检查
- 执行前自动检查目标客户端是否在线
- 返回详细的执行结果和任务ID

## API接口说明

### 1. 指定客户端执行脚本 (请求体方式)

- **接口地址**: `POST /api/client-script/execute`
- **功能描述**: 指定客户端ID和脚本ID，执行脚本库中的脚本
- **请求参数**:
  ```json
  {
    "clientId": "客户端唯一标识",
    "scriptId": "脚本库中的脚本ID"
  }
  ```
- **成功响应**:
  ```json
  {
    "code": 200,
    "message": "脚本已发送至指定客户端执行",
    "data": {
      "clientId": "客户端ID",
      "scriptId": "脚本ID",
      "scriptName": "脚本名称",
      "taskId": "任务ID"
    }
  }
  ```
- **错误响应**:
  - 客户端不在线: `{ "code": 400, "message": "指定的客户端不在线" }`
  - 脚本不存在: `{ "code": 400, "message": "脚本不存在" }`

### 2. 指定客户端执行脚本 (路径参数方式)

- **接口地址**: `POST /api/client-script/clients/{clientId}/scripts/{scriptId}/execute`
- **功能描述**: 通过路径参数指定客户端ID和脚本ID，执行脚本库中的脚本
- **路径参数**:
  - `{clientId}`: 客户端唯一标识
  - `{scriptId}`: 脚本库中的脚本ID
- **成功响应**: 同上

### 3. 获取所有可用脚本

- **接口地址**: `GET /api/client-script/scripts`
- **功能描述**: 获取脚本库中所有可用的脚本
- **响应**: 返回所有脚本对象的数组

### 4. 检查客户端状态

- **接口地址**: `GET /api/client-script/clients/{clientId}/status`
- **功能描述**: 检查指定客户端是否在线
- **路径参数**: `{clientId}` - 客户端唯一标识
- **响应**:
  ```json
  {
    "code": 200,
    "message": "客户端在线",
    "data": {
      "clientId": "客户端ID",
      "isConnected": true
    }
  }
  ```

## WebSocket消息协议

### 脚本执行命令消息格式

``json
{
  "type": "execute_script",
  "payload": {
    "taskId": "任务ID",
    "scriptId": "脚本ID",
    "scriptContent": "脚本内容"
  },
  "clientId": "客户端ID",
  "timestamp": 时间戳
}
```

## 技术实现

### 1. 控制器层
- 新增 `ClientScriptController` 控制器
- 实现多种方式的客户端脚本执行接口

### 2. 服务层
- 利用现有的 `WebSocketService` 和 `CrawlerScriptService`
- 通过 `CrawlerWebSocketHandler` 发送执行命令

### 3. WebSocket通信
- 使用 `sendExecuteScriptCommand` 方法向指定客户端发送脚本执行命令
- 客户端收到命令后执行相应脚本

## 安全考虑

- 需要管理员权限才能执行此功能
- 验证客户端和脚本的存在性
- 防止无效请求造成系统异常

## 使用场景

1. **定向任务分配**: 需要特定客户端执行特定脚本任务
2. **调试与测试**: 手动触发特定客户端执行脚本进行测试
3. **运维管理**: 管理员可远程控制客户端执行特定任务
4. **负载均衡**: 根据客户端状态分配执行任务

## 附录

### 与现有功能的关系

- 与 `/api/admin` 管理端接口配合使用
- 共享 WebSocket 连接管理和脚本库功能
- 与现有的客户端管理和脚本管理功能无缝集成

# 爬虫助手系统 - 终极说明文档

## 项目概述

爬虫助手系统是一个分布式爬虫管理系统，包含Spring Boot后端服务和Chrome浏览器扩展客户端，通过WebSocket实现双向实时通信。系统采用现代化技术栈，支持脚本下发、任务调度、客户端管理等功能。

## 技术架构

### 后端技术栈
- **Spring Boot 2.7.14**: 主框架
- **Spring Web**: Web服务支持
- **Spring Data Redis**: 缓存管理
- **Spring WebSocket**: 实时通信
- **MyBatis-Plus 3.5.3.1**: ORM框架
- **MySQL 8.0.33**: 数据存储
- **Druid 1.2.16**: 数据库连接池
- **JWT**: 身份认证
- **FastJSON**: JSON处理
- **Lombok**: 代码简化
- **Spring Security**: 安全控制
- **SpringDoc + Knife4j**: API文档

### 前端技术栈
- **Chrome Extension Manifest V3**: 浏览器扩展规范
- **JavaScript**: 核心逻辑实现
- **Chrome APIs**: 扩展功能接口

## 系统功能

### 1. 用户认证
- JWT Token认证机制
- 用户登录/登出功能
- Token自动刷新

### 2. WebSocket实时通信
- 客户端与服务端双向通信
- 自动重连机制
- 心跳检测功能

### 3. 脚本管理
- JS爬取脚本的增删改查
- 脚本批量下发功能
- 脚本定向下发功能
- 域名-脚本映射机制

### 4. 任务管理
- 爬虫任务创建与执行
- 任务状态跟踪
- 任务结果存储

### 5. 客户端管理
- 客户端注册与状态管理
- 客户端健康检查
- 客户端空闲状态监控
- 自动移除不健康客户端

### 6. 定时任务
- 服务端统一管理定时任务
- 基于时间间隔的任务调度
- 仅空闲时执行任务选项

## 核心模块详解

### 后端模块

#### 1. 控制器层 (Controllers)
- **AdminController**: 管理员专用接口
- **ExternalTaskController**: 外部任务接口
- **HealthController**: 客户端健康检查
- **IdleController**: 客户端空闲状态管理
- **LoginController**: 用户认证
- **ScheduledTaskController**: 定时任务管理
- **ScriptController**: 爬虫脚本管理

#### 2. 实体层 (Entities)
- **CrawlerUser**: 用户信息
- **CrawlerClient**: 客户端信息
- **CrawlerScript**: 爬取脚本
- **CrawlerTask**: 爬取任务
- **CrawlerResult**: 爬取结果
- **CrawlerScriptPushLog**: 脚本下发日志
- **CrawlerScheduledTask**: 定时任务

#### 3. 数据访问层 (Mappers)
- 基于MyBatis-Plus的CRUD操作
- 支持分页查询
- 自定义SQL查询

#### 4. 服务层 (Services)
- **CrawlerUserService**: 用户管理
- **CrawlerClientService**: 客户端管理
- **CrawlerScriptService**: 脚本管理
- **CrawlerTaskService**: 任务管理
- **CrawlerResultService**: 结果管理
- **WebSocketService**: WebSocket通信
- **ClientHealthCheckService**: 客户端健康检查
- **ClientSelectionService**: 客户端选择策略
- **ScheduledTaskService**: 定时任务管理

#### 5. 工具层 (Utils)
- **JwtUtil**: JWT令牌处理
- **RedisUtil**: Redis操作工具
- **ResponseData**: 统一响应格式

#### 6. 配置层 (Config)
- **MyBatisPlusConfig**: MyBatis-Plus配置
- **OpenApiConfig**: API文档配置
- **RedisConfig**: Redis配置
- **SchedulingConfig**: 定时任务配置
- **WebSocketConfig**: WebSocket配置
- **SecurityConfig**: 安全配置

#### 7. WebSocket处理器
- **CrawlerWebSocketHandler**: WebSocket连接处理
- **WebSocketConfigurator**: WebSocket配置器

### 前端模块

#### 1. 扩展结构
- **manifest.json**: 扩展配置文件
- **background.js**: 后台服务脚本
- **content.js**: 内容脚本
- **popup.html**: 弹出窗口界面
- **login.js**: 登录逻辑
- **task-manager.html**: 任务管理界面
- **task-manager.js**: 任务管理逻辑

#### 2. 功能模块
- **身份认证**: JWT令牌管理
- **WebSocket通信**: 与后端实时通信
- **脚本执行**: 在页面上下文中执行爬取脚本
- **域名匹配**: 自动匹配域名与脚本
- **空闲检测**: 检测用户空闲状态
- **定时任务**: Chrome Alarms API实现

## 数据库设计

### 表结构
1. **crawler_user**: 用户表
2. **crawler_client**: 客户端表
3. **crawler_script**: 爬取脚本表
4. **crawler_task**: 爬取任务表
5. **crawler_result**: 爬取结果表
6. **crawler_script_push_log**: 脚本下发日志表
7. **crawler_scheduled_task**: 定时任务表

### 初始化数据
- 默认管理员账户: admin/123456
- 示例爬取脚本: 电商商品和资讯文章爬取脚本
- 示例定时任务: 淘宝商品5分钟定时爬取

## API文档

系统集成了现代化的API文档工具，可通过以下地址访问：
- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui/index.html`

## 部署指南

### 环境要求
- Java 8+
- Maven 3.6+
- MySQL 5.7+
- Redis

### 部署步骤
1. 修改 `application.yml` 中的数据库连接信息
2. 执行 `schema.sql` 创建数据库表结构
3. 运行 `mvn spring-boot:run` 启动后端服务
4. 安装Chrome扩展到浏览器

### 配置说明
- 服务端口: 8080 (可在application.yml中修改)
- 数据库配置: MySQL连接信息
- Redis配置: 缓存服务连接信息
- JWT配置: 认证密钥和过期时间

## 安全机制

### 认证
- JWT Token认证
- 自动Token刷新
- 登录状态管理

### 授权
- 接口访问权限控制
- 管理员权限验证
- 客户端认证机制

### 数据安全
- 敏感信息加密存储
- SQL注入防护
- XSS攻击防护

## 监控与维护

### 健康检查
- 客户端心跳检测
- 自动移除不健康客户端
- 实时状态监控

### 日志管理
- 脚本下发日志
- 任务执行日志
- 错误日志记录

### 性能优化
- Redis缓存机制
- WebSocket连接复用
- 数据库查询优化

## 扩展功能

### 空闲客户端监控
- 检测客户端空闲状态
- 支持空闲时执行任务
- 可配置空闲阈值

### 定时任务管理
- 服务端统一管理
- 支持多种执行策略
- 任务状态跟踪

### 健康状态管理
- 自动检测客户端健康状态
- 定期清理不健康客户端
- 实时健康状态查询

## 开发指南

### 代码结构
```
src/main/java/smartebao/guide/
├── config/          # 配置类
├── controller/      # 控制器
├── entity/          # 实体类
├── mapper/          # 数据访问层
├── service/         # 服务层
│   └── impl/        # 服务实现
├── utils/           # 工具类
├── websocket/       # WebSocket相关
└── CrawlerApplication.java  # 启动类
```

### 扩展开发
- 新增API接口: 在controller包下创建新的控制器
- 新增数据模型: 在entity包下创建实体类并添加Mapper
- 新增业务逻辑: 在service包下创建服务接口和实现
- 新增配置: 在config包下创建配置类

### 测试
- 单元测试: 使用JUnit进行测试
- 集成测试: 测试完整业务流程
- 压力测试: 评估系统性能

## 常见问题

### 连接问题
- 检查WebSocket连接状态
- 验证JWT令牌有效性
- 确认网络连通性

### 性能问题
- 优化数据库查询
- 调整Redis缓存策略
- 监控系统资源使用

### 安全问题
- 定期更换JWT密钥
- 监控异常访问行为
- 更新依赖库版本

## 项目特点

1. **现代化架构**: 采用Spring Boot + Vue.js技术栈
2. **实时通信**: WebSocket实现双向实时通信
3. **高可用性**: 健康检查和自动恢复机制
4. **易扩展性**: 模块化设计便于功能扩展
5. **安全性强**: JWT认证和权限控制
6. **易维护性**: 清晰的代码结构和完善的文档

## 技术亮点

1. **API文档**: 集成SpringDoc + Knife4j现代化文档工具
2. **智能调度**: 基于域名的自动脚本匹配
3. **空闲执行**: 支持空闲状态下执行爬取任务
4. **健康监控**: 自动检测和清理不健康客户端
5. **任务管理**: 统一的定时任务管理机制
6. **实时通信**: 双向实时通信保障任务执行

## 总结

爬虫助手系统是一个功能完整、架构清晰、易于扩展的分布式爬虫管理系统。系统具备现代化的技术栈、完善的安全机制、强大的功能特性和良好的可维护性，能够满足复杂的企业级爬虫管理需求。