package smartebao.guide.websocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.WebApplicationContextUtils;
import smartebao.guide.entity.CrawlerResult;
import smartebao.guide.entity.CrawlerScript;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.mapper.CrawlerResultMapper;
import smartebao.guide.service.ClientCacheService;
import smartebao.guide.service.WebSocketService;
import smartebao.guide.utils.JwtUtil;
import smartebao.guide.utils.LogUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/ws", configurator = WebSocketConfigurator.class)
@Component
@Slf4j
public class CrawlerWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerWebSocketHandler.class);

    // 存储在线客户端会话
    private static Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    
    // 存储客户端信息
    private static Map<String, String> clientInfoMap = new ConcurrentHashMap<>();

    // 使用静态变量存储必要的实例
    private static JwtUtil jwtUtil;
    private static CrawlerClientMapper crawlerClientMapper;
    private static CrawlerResultMapper crawlerResultMapper;
    private static WebSocketService webSocketService;
    private static ClientCacheService clientCacheService;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "onOpen", session.getId());
        
        try {
            // 从session中获取JWT token进行验证
            String token = getSessionAttribute(session, "token");

            
            // 检查token是否在握手阶段已验证
            Object tokenValidObj = session.getUserProperties().get("token_valid");
            boolean tokenValid = tokenValidObj instanceof Boolean ? (Boolean) tokenValidObj : true;
            
            // 确保jwtUtil已初始化
            if (jwtUtil == null) {
                // 从WebSocketConfigurator获取Bean
                jwtUtil = WebSocketConfigurator.getJwtUtil();
                crawlerClientMapper = WebSocketConfigurator.getCrawlerClientMapper();
                crawlerResultMapper = WebSocketConfigurator.getCrawlerResultMapper();
                webSocketService = WebSocketConfigurator.getWebSocketService();
                clientCacheService = WebSocketConfigurator.getClientCacheService();
            }
            
            if (jwtUtil == null) {
                try {
                    // 如果仍然无法获取jwtUtil，关闭连接
                    CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Authentication service unavailable");
                    session.close(closeReason);
                    LogUtils.logError("JWT验证服务不可用，已关闭连接", null);
                    return;
                } catch (IOException e) {
                    LogUtils.logError("关闭WebSocket连接时发生异常", e);
                }
            }
            
            // 确保所有服务实例已初始化
            if (clientCacheService == null) {
                clientCacheService = WebSocketConfigurator.getClientCacheService();
            }
            if (webSocketService == null) {
                webSocketService = WebSocketConfigurator.getWebSocketService();
            }
            if (crawlerClientMapper == null) {
                crawlerClientMapper = WebSocketConfigurator.getCrawlerClientMapper();
            }
            if (crawlerResultMapper == null) {
                crawlerResultMapper = WebSocketConfigurator.getCrawlerResultMapper();
            }
            
            if (!tokenValid || token == null || !jwtUtil.validateToken(token)) {
                try {
                    // 发送403错误码并关闭连接
                    CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication failed");
                    session.close(closeReason);
                    LogUtils.logWarning("WebSocket连接认证失败，已关闭连接 - token有效: " + tokenValid + ", token存在: " + (token != null));
                    return;
                } catch (IOException e) {
                    LogUtils.logError("关闭WebSocket连接时发生异常", e);
                }
            }
            
            LogUtils.logInfo("WebSocket连接建立成功 - token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            
            // 从token中提取用户名
            String username = jwtUtil.getUsernameFromToken(token);

            // 由于clientId将由客户端插件提供，我们暂时使用一个临时ID直到收到register消息
            String tempClientId = "temp_" + session.getId(); // 使用会话ID作为临时ID
            
            LogUtils.logInfo("WebSocket连接建立成功，等待客户端发送clientId - tempClientId: " + tempClientId + ", username: " + username);
            
            // 发送认证成功消息，通知客户端可以进行注册
            AuthSuccessMessage authMsg = new AuthSuccessMessage();
            authMsg.setType("auth_success");
            authMsg.setPayload(new AuthSuccessPayload(null)); // 不包含实际的clientId，因为将由客户端提供
            authMsg.setClientId(tempClientId);
            authMsg.setTimestamp(System.currentTimeMillis());
            sendMessage(session, JSON.toJSONString(authMsg));
            
            // 临时将临时ID存储在session中，等待register消息
            session.getUserProperties().put("tempClientId", tempClientId);
            
            LogUtils.logInfo("已发送认证成功消息，等待客户端注册 - tempClientId: " + tempClientId + ", username: " + username);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "onOpen", "Connection established");
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "onClose", session.getId());
        
        try {
            String clientId = getSessionAttribute(session, "clientId");
            if (clientId != null) {
                sessionMap.remove(clientId);
                clientInfoMap.remove(clientId);
                
                // 更新客户端状态为offline
                webSocketService.updateClientStatus(clientId, "offline");
                
                LogUtils.logInfo("客户端 " + clientId + " 已断开连接");
            } else {
                LogUtils.logInfo("WebSocket连接已关闭，但未找到对应的clientId");
            }
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "onClose", "Connection closed");
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 收到客户端消息后的处理
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "onMessage", message);
        
        try {
            // 解析消息
            MessageInfo msgInfo = JSON.parseObject(message, MessageInfo.class);
            String type = msgInfo.getType();
            String clientId = msgInfo.getClientId();

            System.out.println("Received message: " + message);
            System.out.println("Message type: " + type);
            System.out.println("Client ID: " + clientId);

            switch (type) {
                case "register":
                    handleRegister(msgInfo, session);
                    break;
                case "url_change":
                    handleUrlChange(msgInfo);
                    break;
                case "crawl_result":
                    handleCrawlResult(msgInfo);
                    break;
                case "idle_status_update":
                    handleIdleStatusUpdate(msgInfo);
                    break;
                case "heartbeat":
                    handleHeartbeat(msgInfo);
                    break;
                case "pong":
                    handlePong(msgInfo);
                    break;
                case "execute_script_response":
                    handleExecuteScriptResponse(msgInfo);
                    break;
                case "client_disconnect":
                    handleClientDisconnect(msgInfo, session);
                    break;
                default:
                    LogUtils.logWarning("收到未知类型消息: " + type);
                    break;
            }
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "onMessage", "Message processed");
        } catch (Exception e) {
            LogUtils.logError("处理WebSocket消息时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 错误发生时的处理
     */
    @OnError
    public void onError(Session session, Throwable error) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "onError", session.getId());
        
        try {
            LogUtils.logError("WebSocket发生错误: " + error.getMessage(), error);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理客户端注册
     */
    private void handleRegister(MessageInfo msgInfo, Session session) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handleRegister", msgInfo.getClientId());
        
        try {
            RegisterPayload payload = JSON.toJavaObject((JSON) JSON.toJSON(msgInfo.getPayload()), RegisterPayload.class);
            String providedClientId = msgInfo.getClientId();

            String username = payload.getUsername();
            String currentUrl = payload.getCurrentUrl();
            String supportTaskTypes = payload.getSupportTaskTypes();
            Boolean idleStatus = payload.getIdleStatus(); // 获取初始空闲状态

            // 保存或更新客户端信息 注释掉 在登陆和登出接口即可
            smartebao.guide.entity.CrawlerClient client = crawlerClientMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<smartebao.guide.entity.CrawlerClient>()
                    .eq("client_id", providedClientId)
                        .eq("username",username)
                        .eq("status","online"));

            // 存储会话，使用客户端提供的clientId
            sessionMap.put(providedClientId, session);
            clientInfoMap.put(providedClientId, username);

            // 更新缓存
            clientCacheService.cacheClientInfo(client);
            clientCacheService.setClientOnlineStatus(providedClientId, true);
            clientCacheService.setClientIdleStatus(providedClientId, idleStatus != null ? idleStatus : false);

            // 批量下发脚本
            sendScriptsToClient(providedClientId, "batch");

            LogUtils.logInfo("客户端 " + providedClientId + " 注册成功，空闲状态: " + (idleStatus != null ? idleStatus : false));
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handleRegister", "Client registered");
        } catch (Exception e) {
            LogUtils.logError("处理客户端注册时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理网址变化
     */
    private void handleUrlChange(MessageInfo msgInfo) {
        LogUtils.generateRequestId(); // 生成請求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handleUrlChange", msgInfo.getClientId());
        
        try {
            UrlChangePayload payload = JSON.toJavaObject((JSON) JSON.toJSON(msgInfo.getPayload()), UrlChangePayload.class);
            String clientId = msgInfo.getClientId();
            String currentUrl = payload.getCurrentUrl();

            // 更新客户端网址
            webSocketService.updateClientUrl(clientId, currentUrl);

            LogUtils.logInfo("客户端 " + clientId + " 网址更新为: " + currentUrl);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handleUrlChange", "URL updated");
        } catch (Exception e) {
            LogUtils.logError("处理网址变化时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理爬取结果
     */
    private void handleCrawlResult(MessageInfo msgInfo) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handleCrawlResult", msgInfo.getClientId());
        
        try {
            CrawlResultPayload payload = JSON.toJavaObject((JSON) JSON.toJSON(msgInfo.getPayload()), CrawlResultPayload.class);
            String clientId = msgInfo.getClientId();
            String taskId = payload.getTaskId();
            Object crawlData = payload.getCrawlData();
            String crawlStatus = payload.getCrawlStatus();

            // 保存爬取结果
            webSocketService.saveCrawlResult(taskId, clientId, crawlData, crawlStatus);

            LogUtils.logInfo("收到客户端 " + clientId + " 的爬取结果，任务ID: " + taskId);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handleCrawlResult", "Crawl result processed");
        } catch (Exception e) {
            LogUtils.logError("处理爬取结果时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理空闲状态更新
     */
    private void handleIdleStatusUpdate(MessageInfo msgInfo) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handleIdleStatusUpdate", msgInfo.getClientId());
        
        try {
            IdleStatusUpdatePayload payload = JSON.toJavaObject((JSON) JSON.toJSON(msgInfo.getPayload()), IdleStatusUpdatePayload.class);
            String clientId = msgInfo.getClientId();
            Boolean isIdle = payload.getIsIdle();
            Long timestamp = payload.getTimestamp();
            Long idleDuration = payload.getIdleDuration();

            // 更新客户端空闲状态
            webSocketService.updateClientIdleStatus(clientId, isIdle);

            LogUtils.logInfo("客户端 " + clientId + " 空闲状态更新: " + isIdle + 
                             ", 空闲持续时间: " + (idleDuration != null ? idleDuration + "ms" : "N/A"));

            // 更新缓存
            clientCacheService.setClientIdleStatus(clientId, isIdle);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handleIdleStatusUpdate", "Idle status updated");
        } catch (Exception e) {
            LogUtils.logError("处理空闲状态更新时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(MessageInfo msgInfo) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handleHeartbeat", msgInfo.getClientId());
        
        try {
            String clientId = msgInfo.getClientId();
            Long timestamp = msgInfo.getTimestamp();
            
            // 更新心跳时间到缓存
            webSocketService.updateClientHeartbeat(clientId);
            
            LogUtils.logInfo("收到客户端 " + clientId + " 的心跳，时间戳: " + new Date(timestamp));
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handleHeartbeat", "Heartbeat processed");
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理Pong回应
     */
    private void handlePong(MessageInfo msgInfo) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handlePong", msgInfo.getClientId());
        
        try {
            String clientId = msgInfo.getClientId();
            Long timestamp = msgInfo.getTimestamp();
            PongPayload payload = JSON.toJavaObject((JSON) JSON.toJSON(msgInfo.getPayload()), PongPayload.class);
            
            LogUtils.logInfo("收到客户端 " + clientId + " 的Pong回应，请求ID: " + payload.getRequestId() + 
                             "，时间戳: " + new Date(timestamp));
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handlePong", "Pong processed");
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理脚本执行响应
     */
    private void handleExecuteScriptResponse(MessageInfo msgInfo) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handleExecuteScriptResponse", msgInfo.getClientId());
        
        try {
            ExecuteScriptResponsePayload payload = JSON.toJavaObject((JSON) JSON.toJSON(msgInfo.getPayload()), ExecuteScriptResponsePayload.class);
            String clientId = msgInfo.getClientId();
            String taskId = payload.getTaskId();
            Object result = payload.getResult();
            String status = payload.getStatus();
            String error = payload.getError();

            // 保存执行结果
            webSocketService.saveCrawlResult(taskId, clientId, result, status);

            LogUtils.logInfo("收到客户端 " + clientId + " 的脚本执行响应，任务ID: " + taskId + "，状态: " + status);

            // 如果有错误，打印错误信息
            if ("error".equals(status) && error != null) {
                LogUtils.logError("脚本执行错误: " + error, null);
            }
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handleExecuteScriptResponse", "Script response processed");
        } catch (Exception e) {
            LogUtils.logError("处理脚本执行响应时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 处理客户端断开连接请求
     */
    private void handleClientDisconnect(MessageInfo msgInfo, Session session) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "handleClientDisconnect", msgInfo.getClientId());
        
        try {
            ClientDisconnectPayload payload = JSON.toJavaObject((JSON) JSON.toJSON(msgInfo.getPayload()), ClientDisconnectPayload.class);
            String clientId = msgInfo.getClientId();
            String reason = payload.getReason();
            Long timestamp = payload.getTimestamp();

            LogUtils.logInfo("收到客户端 " + clientId + " 的断开连接请求，原因: " + reason + ", 时间戳: " + new Date(timestamp));
            
            // 从会话映射中移除客户端
            sessionMap.remove(clientId);
            clientInfoMap.remove(clientId);
            
            // 更新客户端状态为offline
            webSocketService.updateClientStatus(clientId, "offline");
            
            // 更新缓存状态
            if (clientCacheService != null) {
                clientCacheService.setClientOnlineStatus(clientId, false);
            }
            
            LogUtils.logInfo("客户端 " + clientId + " 已处理断开连接请求");
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "handleClientDisconnect", "Client disconnect processed");
        } catch (Exception e) {
            LogUtils.logError("处理客户端断开连接请求时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 批量下发脚本到客户端
     */
    public void sendScriptsToClient(String clientId, String pushType) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "sendScriptsToClient", clientId);
        
        try {
            Session session = sessionMap.get(clientId);
            if (session == null) {
                LogUtils.logInfo("客户端 " + clientId + " 不在线，无法下发脚本");
                return;
            }

            // 查询所有脚本
            List<CrawlerScript> scripts = webSocketService.getAllScripts();

            // 构造脚本下发消息
            ScriptPushMessage scriptMsg = new ScriptPushMessage();
            scriptMsg.setType("script_push");
            scriptMsg.setPayload(new ScriptPushPayload(scripts, pushType));
            scriptMsg.setClientId(clientId);
            scriptMsg.setTimestamp(System.currentTimeMillis());

            sendMessage(session, JSON.toJSONString(scriptMsg));
            LogUtils.logInfo("已向客户端 " + clientId + " 下发 " + scripts.size() + " 个脚本");
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "sendScriptsToClient", "Scripts sent");
        } catch (Exception e) {
            LogUtils.logError("向客户端下发脚本时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 定向下发脚本到指定客户端
     */
    public void sendDesignatedScriptsToClient(String clientId, List<String> scriptIds) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "sendDesignatedScriptsToClient", clientId);
        
        try {
            Session session = sessionMap.get(clientId);
            if (session == null) {
                LogUtils.logInfo("客户端 " + clientId + " 不在线，无法定向下发脚本");
                return;
            }

            // 根据脚本ID列表查询脚本
            List<CrawlerScript> scripts = webSocketService.getScriptsByIds(scriptIds);

            // 构造定向脚本下发消息
            ScriptPushMessage scriptMsg = new ScriptPushMessage();
            scriptMsg.setType("script_designated_push");
            scriptMsg.setPayload(new ScriptPushPayload(scripts, "designated"));
            scriptMsg.setClientId(clientId);
            scriptMsg.setTimestamp(System.currentTimeMillis());

            sendMessage(session, JSON.toJSONString(scriptMsg));
            LogUtils.logInfo("已向客户端 " + clientId + " 定向下發 " + scripts.size() + " 个脚本");
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "sendDesignatedScriptsToClient", "Designated scripts sent");
        } catch (Exception e) {
            LogUtils.logError("向客户端定向下发脚本时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 发送任务指令到客户端
     */
    public void sendTaskCommand(String clientId, String taskId, String scriptId, Boolean executeOnIdle) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "sendTaskCommand", clientId);
        
        try {
            Session session = sessionMap.get(clientId);
            if (session == null) {
                LogUtils.logInfo("客户端 " + clientId + " 不在线，无法发送任务指令");
                return;
            }

            // 构造任务指令消息
            TaskCommandMessage taskMsg = new TaskCommandMessage();
            taskMsg.setType("task_command");
            taskMsg.setPayload(new TaskCommandPayload(taskId, scriptId, executeOnIdle));
            taskMsg.setClientId(clientId);
            taskMsg.setTimestamp(System.currentTimeMillis());

            sendMessage(session, JSON.toJSONString(taskMsg));
            LogUtils.logInfo("已向客户端 " + clientId + " 发送任务指令，任务ID: " + taskId);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "sendTaskCommand", "Task command sent");
        } catch (Exception e) {
            LogUtils.logError("向客户端发送任务指令时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 发送脚本执行命令到客户端
     */
    public void sendExecuteScriptCommand(String clientId, String taskId, String scriptId, String scriptContent) {
        LogUtils.generateRequestId(); // 生成请求ID
        LogUtils.logMethodEntry(this.getClass().getSimpleName(), "sendExecuteScriptCommand", clientId);
        
        try {
            Session session = sessionMap.get(clientId);
            if (session == null) {
                LogUtils.logInfo("客户端 " + clientId + " 不在线，无法发送脚本执行命令");
                return;
            }

            // 构造脚本执行命令消息
            ExecuteScriptCommandMessage scriptMsg = new ExecuteScriptCommandMessage();
            scriptMsg.setType("execute_script");
            scriptMsg.setPayload(new ExecuteScriptCommandPayload(taskId, scriptId, scriptContent));
            scriptMsg.setClientId(clientId);
            scriptMsg.setTimestamp(System.currentTimeMillis());

            sendMessage(session, JSON.toJSONString(scriptMsg));
            LogUtils.logInfo("已向客户端 " + clientId + " 发送脚本执行命令，任务ID: " + taskId);
            LogUtils.logMethodExit(this.getClass().getSimpleName(), "sendExecuteScriptCommand", "Execute script command sent");
        } catch (Exception e) {
            LogUtils.logError("向客户端发送脚本执行命令时发生异常", e);
        } finally {
            LogUtils.clearMDC(); // 清理MDC
        }
    }

    /**
     * 发送消息到指定会话
     */
    private void sendMessage(Session session, String message) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
                logger.debug("发送消息到会话: {}", session.getId());
            } else {
                logger.warn("尝试向已关闭的会话发送消息: {}", session.getId());
            }
        } catch (IOException e) {
            logger.error("发送消息时发生IO异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取会话属性
     */
    private String getSessionAttribute(Session session, String key) {
        Object obj = session.getUserProperties().get(key);
        return obj != null ? obj.toString() : null;
    }

    /**
     * 获取在线客户端数量
     */
    public static int getOnlineCount() {
        logger.debug("当前在线客户端数量: {}", sessionMap.size());
        return sessionMap.size();
    }

    /**
     * 获取指定客户端的会话
     */
    public static Session getClientSession(String clientId) {
        logger.debug("获取客户端 {} 的会话", clientId);
        return sessionMap.get(clientId);
    }

    /**
     * 检查客户端是否在线
     */
    public static boolean isClientOnline(String clientId) {
        Session session = sessionMap.get(clientId);
        logger.debug("客户端 {} 在线状态: {}", clientId, session != null && session.isOpen());
        return session != null && session.isOpen();
    }

    /**
     * 获取会话映射
     */
    public static Map<String, Session> getSessionMap() {
        logger.debug("获取会话映射，当前连接数: {}", sessionMap.size());
        return sessionMap;
    }

    // 内部消息类定义
    public static class MessageInfo {
        private String type;
        private Object payload;
        private String clientId;
        private Long timestamp;

        // getter和setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class RegisterPayload {
        private String username;
        private String currentUrl;
        private String supportTaskTypes;
        private Boolean idleStatus; // 新增：空闲状态
        private String clientId; // 新增：客户端ID

        // getter和setter
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getCurrentUrl() { return currentUrl; }
        public void setCurrentUrl(String currentUrl) { this.currentUrl = currentUrl; }
        public String getSupportTaskTypes() { return supportTaskTypes; }
        public void setSupportTaskTypes(String supportTaskTypes) { this.supportTaskTypes = supportTaskTypes; }
        public Boolean getIdleStatus() { return idleStatus; }
        public void setIdleStatus(Boolean idleStatus) { this.idleStatus = idleStatus; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
    }

    public static class UrlChangePayload {
        private String currentUrl;

        // getter和setter
        public String getCurrentUrl() { return currentUrl; }
        public void setCurrentUrl(String currentUrl) { this.currentUrl = currentUrl; }
    }

    public static class CrawlResultPayload {
        private String taskId;
        private Object crawlData;
        private String crawlStatus;

        // getter和setter
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public Object getCrawlData() { return crawlData; }
        public void setCrawlData(Object crawlData) { this.crawlData = crawlData; }
        public String getCrawlStatus() { return crawlStatus; }
        public void setCrawlStatus(String crawlStatus) { this.crawlStatus = crawlStatus; }
    }

    public static class IdleStatusUpdatePayload {
        private Boolean isIdle;
        private Long timestamp;
        private Long idleDuration;

        // getter和setter
        public Boolean getIsIdle() { return isIdle; }
        public void setIsIdle(Boolean isIdle) { this.isIdle = isIdle; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        public Long getIdleDuration() { return idleDuration; }
        public void setIdleDuration(Long idleDuration) { this.idleDuration = idleDuration; }
    }

    public static class PongPayload {
        private String requestId;
        private Long timestamp;

        // getter和setter
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class ExecuteScriptResponsePayload {
        private String taskId;
        private Object result;
        private String status;
        private String error;

        // getter和setter
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class ClientDisconnectPayload {
        private String reason;
        private Long timestamp;

        // getter和setter
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class AuthSuccessMessage {
        private String type;
        private AuthSuccessPayload payload;
        private String clientId;
        private Long timestamp;

        // getter和setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public AuthSuccessPayload getPayload() { return payload; }
        public void setPayload(AuthSuccessPayload payload) { this.payload = payload; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class AuthSuccessPayload {
        private String clientId;

        public AuthSuccessPayload(String clientId) {
            this.clientId = clientId;
        }

        // getter和setter
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
    }

    public static class ScriptPushMessage {
        private String type;
        private ScriptPushPayload payload;
        private String clientId;
        private Long timestamp;

        // getter和setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public ScriptPushPayload getPayload() { return payload; }
        public void setPayload(ScriptPushPayload payload) { this.payload = payload; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class ScriptPushPayload {
        private List<CrawlerScript> scripts;
        private String pushType;

        public ScriptPushPayload(List<CrawlerScript> scripts, String pushType) {
            this.scripts = scripts;
            this.pushType = pushType;
        }

        // getter和setter
        public List<CrawlerScript> getScripts() { return scripts; }
        public void setScripts(List<CrawlerScript> scripts) { this.scripts = scripts; }
        public String getPushType() { return pushType; }
        public void setPushType(String pushType) { this.pushType = pushType; }
    }

    public static class TaskCommandMessage {
        private String type;
        private TaskCommandPayload payload;
        private String clientId;
        private Long timestamp;

        // getter和setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public TaskCommandPayload getPayload() { return payload; }
        public void setPayload(TaskCommandPayload payload) { this.payload = payload; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class TaskCommandPayload {
        private String taskId;
        private String scriptId;
        private Boolean executeOnIdle;

        public TaskCommandPayload(String taskId, String scriptId, Boolean executeOnIdle) {
            this.taskId = taskId;
            this.scriptId = scriptId;
            this.executeOnIdle = executeOnIdle;
        }

        // getter和setter
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getScriptId() { return scriptId; }
        public void setScriptId(String scriptId) { this.scriptId = scriptId; }
        public Boolean getExecuteOnIdle() { return executeOnIdle; }
        public void setExecuteOnIdle(Boolean executeOnIdle) { this.executeOnIdle = executeOnIdle; }
    }

    public static class ExecuteScriptCommandMessage {
        private String type;
        private ExecuteScriptCommandPayload payload;
        private String clientId;
        private Long timestamp;

        // getter和setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public ExecuteScriptCommandPayload getPayload() { return payload; }
        public void setPayload(ExecuteScriptCommandPayload payload) { this.payload = payload; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }

    public static class ExecuteScriptCommandPayload {
        private String taskId;
        private String scriptId;
        private String scriptContent;

        public ExecuteScriptCommandPayload(String taskId, String scriptId, String scriptContent) {
            this.taskId = taskId;
            this.scriptId = scriptId;
            this.scriptContent = scriptContent;
        }

        // getter和setter
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getScriptId() { return scriptId; }
        public void setScriptId(String scriptId) { this.scriptId = scriptId; }
        public String getScriptContent() { return scriptContent; }
        public void setScriptContent(String scriptContent) { this.scriptContent = scriptContent; }
    }
    
//    public static class ClientDisconnectPayload {
//        private String reason;
//        private Long timestamp;
//
//        // getter和setter
//        public String getReason() { return reason; }
//        public void setReason(String reason) { this.reason = reason; }
//        public Long getTimestamp() { return timestamp; }
//        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
//    }


}