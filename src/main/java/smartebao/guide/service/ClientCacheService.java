package smartebao.guide.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.utils.RedisUtil;

import java.util.concurrent.TimeUnit;

@Service
public class ClientCacheService {

    @Autowired
    private RedisUtil redisUtil;

    private static final String CLIENT_PREFIX = "client:";
    private static final String CLIENT_ONLINE_STATUS_PREFIX = "client:online:";
    private static final String CLIENT_IDLE_STATUS_PREFIX = "client:idle:";
    private static final String CLIENT_HEARTBEAT_PREFIX = "client:heartbeat:";

    /**
     * 缓存客户端信息
     */
    public void cacheClientInfo(CrawlerClient client) {
        String key = CLIENT_PREFIX + client.getClientId();
        redisUtil.set(key, client, 3600); // 缓存1小时
    }

    /**
     * 获取客户端信息
     */
    public CrawlerClient getClientInfo(String clientId) {
        String key = CLIENT_PREFIX + clientId;
        return (CrawlerClient) redisUtil.get(key);
    }

    /**
     * 删除客户端缓存
     */
    public void removeClientCache(String clientId) {
        String key = CLIENT_PREFIX + clientId;
        redisUtil.del(key);
    }

    /**
     * 设置客户端在线状态
     */
    public void setClientOnlineStatus(String clientId, boolean isOnline) {
        String key = CLIENT_ONLINE_STATUS_PREFIX + clientId;
        redisUtil.set(key, isOnline, 3600); // 缓存1小时
    }

    /**
     * 获取客户端在线状态
     */
    public Boolean getClientOnlineStatus(String clientId) {
        String key = CLIENT_ONLINE_STATUS_PREFIX + clientId;
        Object value = redisUtil.get(key);
        return value != null ? (Boolean) value : false;
    }

    /**
     * 设置客户端空闲状态
     */
    public void setClientIdleStatus(String clientId, Boolean isIdle) {
        String key = CLIENT_IDLE_STATUS_PREFIX + clientId;
        redisUtil.set(key, isIdle, 3600); // 缓存1小时
    }

    /**
     * 获取客户端空闲状态
     */
    public Boolean getClientIdleStatus(String clientId) {
        String key = CLIENT_IDLE_STATUS_PREFIX + clientId;
        Object value = redisUtil.get(key);
        return value != null ? (Boolean) value : false;
    }

    /**
     * 更新客户端心跳时间
     */
    public void updateClientHeartbeat(String clientId) {
        String key = CLIENT_HEARTBEAT_PREFIX + clientId;
        redisUtil.set(key, System.currentTimeMillis(), 120); // 缓存2分钟，比心跳周期稍长
    }

    /**
     * 获取客户端最后心跳时间
     */
    public Long getClientLastHeartbeat(String clientId) {
        String key = CLIENT_HEARTBEAT_PREFIX + clientId;
        Object value = redisUtil.get(key);
        return value != null ? (Long) value : 0L;
    }

    /**
     * 检查客户端是否健康（在指定时间内有心跳）
     */
    public boolean isClientHealthy(String clientId, long timeoutMs) {
        Long lastHeartbeat = getClientLastHeartbeat(clientId);
        if (lastHeartbeat == null || lastHeartbeat == 0L) {
            return false;
        }
        return (System.currentTimeMillis() - lastHeartbeat) <= timeoutMs;
    }

    /**
     * 设置字符串值到Redis
     */
    public void setString(String key, String value) {
        redisUtil.set(key, value);
    }

    /**
     * 从Redis获取字符串值
     */
    public String getString(String key) {
        Object value = redisUtil.get(key);
        return value != null ? (String) value : null;
    }
}