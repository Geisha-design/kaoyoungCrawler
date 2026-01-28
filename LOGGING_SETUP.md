# 项目日志框架配置指南

## 概述
本项目已集成了完善的日志框架，采用SLF4J + Logback的组合，并结合了结构化日志输出能力。

## 日志框架组成
- **SLF4J**: Java日志门面，提供统一的日志API
- **Logback**: SLF4J的具体实现，性能优秀
- **Logstash-Logback-Encoder**: 提供JSON格式的结构化日志输出
- **LogUtils**: 自定义日志工具类，提供便捷的日志记录方法

## 配置文件说明

### 1. logback-spring.xml
位于 `src/main/resources/logback-spring.xml`，提供以下功能：
- 控制台日志输出（开发环境）
- 文件日志输出（按日期和大小滚动）
- 错误日志单独输出
- JSON格式结构化日志输出
- 多环境支持（dev/test/prod）

### 2. application.yml
在 `src/main/resources/application.yml` 中增加了日志配置：
- 设置不同包的日志级别
- 自定义日志输出格式
- 集成MyBatis SQL日志输出

## 日志级别说明
- **TRACE**: 最详细的信息，通常只在开发期间启用
- **DEBUG**: 详细的调试信息
- **INFO**: 一般信息消息
- **WARN**: 警告信息
- **ERROR**: 错误事件信息，但应用程序能继续运行
- **FATAL**: 严重错误事件，可能导致应用程序中断

## LogUtils 工具类功能

### 主要功能
1. **统一日志记录方法**: 提供标准的日志记录接口
2. **MDC上下文管理**: 实现请求链路追踪
3. **请求ID生成**: 便于问题排查和日志关联

### 使用示例
```java
// 生成请求ID（建议在方法入口处调用）
LogUtils.generateRequestId();

// 记录方法入口日志
LogUtils.logMethodEntry(className, methodName, params);

// 记录方法出口日志
LogUtils.logMethodExit(className, methodName, result);

// 记录错误日志
LogUtils.logError(message, throwable);

// 记录警告日志
LogUtils.logWarning(message);

// 记录普通信息日志
LogUtils.logInfo(message);

// 清理MDC上下文（建议在finally块中调用）
LogUtils.clearMDC();
```

## 日志文件说明
- **kaoyoungCrawler.log**: 所有INFO级别及以上的日志
- **kaoyoungCrawler_error.log**: 所有ERROR级别的日志
- **kaoyoungCrawler_structured.log**: JSON格式的结构化日志
- 日志文件自动按日期和大小滚动，保留30天的历史日志

## 最佳实践

### 1. 在控制器中使用
```java
@RestController
@Slf4j
public class SomeController {
    @GetMapping("/api/endpoint")
    public ResponseEntity<?> someEndpoint() {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "someEndpoint", parameters);
        
        try {
            // 业务逻辑
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogUtils.logError("处理请求时发生异常", e);
            throw e;
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }
}
```

### 2. 在服务层使用
```java
@Service
@Slf4j
public class SomeService {
    public ResultType doSomething(ParamType param) {
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "doSomething", param);
        
        try {
            // 业务逻辑
            ResultType result = businessLogic(param);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "doSomething", result);
            return result;
        } catch (Exception e) {
            LogUtils.logError("执行业务逻辑时发生异常", e);
            throw e;
        }
    }
}
```

### 3. 在WebSocket处理器中使用
```java
@Component
@Slf4j
public class SomeWebSocketHandler {
    @OnOpen
    public void onOpen(Session session) {
        LogUtils.generateRequestId();
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "onOpen", session.getId());
        
        try {
            // 业务逻辑
        } finally {
            LogUtils.clearMDC();
        }
    }
}
```

## 环境配置
- **开发环境(dev)**: 同时输出到控制台和文件，DEBUG级别
- **测试环境(test)**: 只输出到文件，INFO级别
- **生产环境(prod)**: 只输出到文件，WARN级别

## 监控和维护
- 定期清理日志文件以节省磁盘空间
- 监控ERROR日志以及时发现系统问题
- 使用结构化日志进行数据分析和问题排查