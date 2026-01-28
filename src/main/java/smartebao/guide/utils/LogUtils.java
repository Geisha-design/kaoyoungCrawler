package smartebao.guide.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 日志工具类
 * 提供统一的日志记录方法和MDC上下文管理
 */
public class LogUtils {

    private static final Logger log = LoggerFactory.getLogger(LogUtils.class);

    /**
     * MDC中的请求ID键名
     */
    public static final String REQUEST_ID = "requestId";

    /**
     * 记录进入方法的日志
     *
     * @param className 类名
     * @param methodName 方法名
     * @param params 参数
     */
    public static void logMethodEntry(String className, String methodName, Object... params) {
        String requestId = getCurrentRequestId();
        log.info("Entering method: {}.{} with params: {}, RequestId: {}", 
                 className, methodName, params, requestId);
    }

    /**
     * 记录退出方法的日志
     *
     * @param className 类名
     * @param methodName 方法名
     * @param result 返回结果
     */
    public static void logMethodExit(String className, String methodName, Object result) {
        String requestId = getCurrentRequestId();
        log.info("Exiting method: {}.{} with result: {}, RequestId: {}", 
                 className, methodName, result, requestId);
    }

    /**
     * 记录错误信息
     *
     * @param message 错误信息
     * @param throwable 异常对象
     */
    public static void logError(String message, Throwable throwable) {
        String requestId = getCurrentRequestId();
        log.error("Error occurred: {}, RequestId: {}", message, requestId, throwable);
    }

    /**
     * 记录警告信息
     *
     * @param message 警告信息
     */
    public static void logWarning(String message) {
        String requestId = getCurrentRequestId();
        log.warn("Warning: {}, RequestId: {}", message, requestId);
    }

    /**
     * 记录信息
     *
     * @param message 信息内容
     */
    public static void logInfo(String message) {
        String requestId = getCurrentRequestId();
        log.info("{}, RequestId: {}", message, requestId);
    }

    /**
     * 记录调试信息
     *
     * @param message 调试信息
     */
    public static void logDebug(String message) {
        String requestId = getCurrentRequestId();
        log.debug("{}, RequestId: {}", message, requestId);
    }

    /**
     * 生成并设置请求ID
     */
    public static void generateRequestId() {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(REQUEST_ID, requestId);
    }

    /**
     * 获取当前请求ID
     *
     * @return 请求ID
     */
    public static String getCurrentRequestId() {
        String requestId = MDC.get(REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = "N/A";
        }
        return requestId;
    }

    /**
     * 清理MDC上下文
     */
    public static void clearMDC() {
        MDC.clear();
    }

    /**
     * 设置MDC中的自定义属性
     *
     * @param key 属性键
     * @param value 属性值
     */
    public static void setMDCProperty(String key, String value) {
        MDC.put(key, value);
    }

    /**
     * 移除MDC中的特定属性
     *
     * @param key 属性键
     */
    public static void removeMDCProperty(String key) {
        MDC.remove(key);
    }
}