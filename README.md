# 爬虫助手系统后端

基于Spring Boot的爬虫助手系统后端，支持浏览器插件客户端通过WebSocket进行实时通信，实现JS爬取脚本的全局批量下发与指定客户端定向下发。

## 功能特性

- JWT身份认证
- WebSocket双向实时通信
- 支持脚本批量下发和定向下发
- 支持定时任务调度
- 网站爬取调度功能
- 数据持久化至MySQL

## 技术栈

- Spring Boot 2.7.x
- Spring WebSocket
- MyBatis-Plus
- MySQL 8.0+
- JWT
- FastJSON
- Lombok

## 快速开始

### 环境准备

1. JDK 8或11
2. Maven 3.6+
3. MySQL 8.0+

### 配置数据库

1. 创建数据库 `crawler_db`，字符集设为utf8mb4
2. 执行 [schema.sql](src/main/resources/schema.sql) 脚本初始化表结构

### 配置应用

修改 [application.yml](src/main/resources/application.yml) 文件中的数据库连接信息：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/crawler_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: root  # 替换为实际用户名
      password: root  # 替换为实际密码
```

### 运行项目

```bash
# 编译并运行
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/kaoyoungCrawler-1.0-SNAPSHOT.jar
```

## 接口说明

### HTTP接口

#### 登录接口

- 地址：`/api/login`
- 方法：POST
- 请求头：`Content-Type: application/json`
- 请求体：`{ "username": "string", "password": "string" }`
- 返回：`{ "code": 200, "message": "登录成功", "data": { "token": "string" } }`

#### 脚本定向下发接口

- 地址：`/api/script/designated/push`
- 方法：POST
- 请求头：`Content-Type: application/json`
- 请求体：`{ "clientId": "string", "scriptIds": ["string"] }`
- 返回：`{ "code": 200, "message": "定向下发脚本请求已受理" }`

#### 定时任务接口

- 查询定时任务：`/api/scheduled-task/list/{clientId}`
- 同步定时任务：`/api/scheduled-task/sync`

### WebSocket接口

- 地址：`ws://localhost:8080/ws?token=xxx`
- 连接时需携带JWT令牌

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── smartebao/
│   │       └── guide/
│   │           ├── CrawlerApplication.java  # 主启动类
│   │           ├── controller/              # 控制器
│   │           ├── entity/                  # 实体类
│   │           ├── mapper/                  # 数据访问层
│   │           ├── service/                 # 业务逻辑层
│   │           ├── utils/                   # 工具类
│   │           ├── websocket/               # WebSocket相关
│   │           └── config/                  # 配置类
│   └── resources/
│       ├── application.yml                  # 应用配置
│       └── schema.sql                       # 数据库脚本
```

## 数据库表结构

- `crawler_user`: 用户表
- `crawler_client`: 客户端注册表
- `crawler_script`: 爬取脚本表
- `crawler_task`: 爬取任务表
- `crawler_result`: 爬取结果表
- `crawler_script_push_log`: 脚本下发日志表
- `crawler_scheduled_task`: 定时任务表