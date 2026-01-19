# 数据库配置指南

## MySQL数据库设置

### 1. 创建数据库

```sql
CREATE DATABASE crawler_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 2. 配置MySQL用户权限

如果您使用的是root用户，请确保密码正确。或者创建一个新的用户：

```sql
CREATE USER 'crawler_user'@'localhost' IDENTIFIED BY 'crawler_password';
GRANT ALL PRIVILEGES ON crawler_db.* TO 'crawler_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 修改应用配置

编辑 [application.yml](src/main/resources/application.yml) 文件，更新数据库连接信息：

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/crawler_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      username: root  # 替换为您的MySQL用户名
      password: root  # 替换为您的MySQL密码
```

### 4. 初始化表结构

应用启动时会自动执行 [schema.sql](src/main/resources/schema.sql) 脚本来创建表结构和初始数据。