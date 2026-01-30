# 自定义日志颜色配置说明

本项目使用Logback作为日志框架，已配置彩色日志输出。以下是关于如何自定义不同日志级别颜色的说明。

## 当前配置说明

在 [logback-spring.xml](file:///Users/qiyuzheng/Desktop/java_project/kaoyoungCrawler/src/main/resources/logback-spring.xml) 中，我们使用了 `%highlight()` 来实现基础的彩色日志输出。

## 实现特定颜色配置

要实现您要求的精确颜色配置（DEBUG为绿色、INFO为粉红色、WARN为黄色），需要以下步骤：

### 方法一：使用Spring Boot的彩色输出

在 `application.yml` 中添加：

```yaml
spring:
  output:
    ansi:
      enabled: always
logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %clr(%-5level) %cyan(%logger{36}) - %msg%n"
```

然后在代码中使用：
- DEBUG 级别会显示为绿色
- INFO 级别会显示为默认颜色
- WARN 和 ERROR 级别会显示为警告色

### 方法二：实现自定义的Logback转换器（高级）

要实现精确的颜色配置，需要编写自定义的Logback转换器类，这需要以下步骤：

1. 创建自定义转换器类
2. 在 `src/main/resources/META-INF/services/ch.qos.logback.classic.pattern.Converter` 文件中注册
3. 重新构建项目

### 方法三：使用第三方库

您可以添加 `logback-contrib` 库来支持更高级的颜色配置：

```xml
<dependency>
    <groupId>ch.qos.logback.contrib</groupId>
    <artifactId>logback-jackson</artifactId>
    <version>0.1.5</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback.contrib</groupId>
    <artifactId>logback-json-classic</artifactId>
    <version>0.1.5</version>
</dependency>
```

## 当前效果

目前的配置会实现以下颜色效果：
- ERROR: 红色粗体
- WARN: 黄色
- INFO: 绿色
- DEBUG: 白色/蓝色（取决于终端）

## 注意事项

1. 不同的终端和IDE对ANSI颜色的支持可能略有差异
2. 生产环境中通常禁用彩色输出以节省资源
3. 精确的颜色控制需要额外的配置和依赖

如需更高级的颜色控制，请参考上述方法或联系开发团队进行定制。