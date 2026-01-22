package smartebao.guide.websocket;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import smartebao.guide.mapper.CrawlerClientMapper;
import smartebao.guide.mapper.CrawlerResultMapper;
import smartebao.guide.service.ClientCacheService;
import smartebao.guide.service.WebSocketService;
import smartebao.guide.utils.JwtUtil;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;
import java.util.List;

@Component
public class WebSocketConfigurator extends Configurator implements ApplicationContextAware {

    private static JwtUtil jwtUtil;
    private static CrawlerClientMapper crawlerClientMapper;
    private static CrawlerResultMapper crawlerResultMapper;
    private static WebSocketService webSocketService;
    private static ClientCacheService clientCacheService;
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        WebSocketConfigurator.applicationContext = context;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 从握手请求中获取参数
        if (request.getQueryString() != null) {
            String queryString = request.getQueryString();
            String[] params = queryString.split("&");
            String token = null;
            
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    token = keyValue[1];
                    break;
                }
            }
            
            if (token != null) {
                // 验证JWT令牌
                if (validateToken(token)) {
                    // 验证通过，存储token
                    sec.getUserProperties().put("token", token);
                    sec.getUserProperties().put("token_valid", true);
                } else {
                    // 验证失败，标记为无效
                    sec.getUserProperties().put("token_valid", false);
                }
            } else {
                // 没有提供token，标记为无效
                sec.getUserProperties().put("token_valid", false);
            }
        } else {
            // 没有查询参数，标记为无效
            sec.getUserProperties().put("token_valid", false);
        }
    }
    
    private boolean validateToken(String token) {
        // 尝试从Spring上下文获取JwtUtil
        if (jwtUtil == null && applicationContext != null) {
            try {
                jwtUtil = applicationContext.getBean(JwtUtil.class);
            } catch (BeansException e) {
                // 如果无法获取bean，返回false表示验证失败
                return false;
            }
        }
        
        if (jwtUtil != null) {
            return jwtUtil.validateToken(token);
        } else {
            // 如果JwtUtil不可用，返回false表示验证失败
            return false;
        }
    }
    
    // 提供静态方法供WebSocketHandler获取Bean
    public static JwtUtil getJwtUtil() {
        if (jwtUtil == null && applicationContext != null) {
            try {
                jwtUtil = applicationContext.getBean(JwtUtil.class);
            } catch (BeansException e) {
                // 如果无法获取bean，返回null
                return null;
            }
        }
        return jwtUtil;
    }
    
    public static CrawlerClientMapper getCrawlerClientMapper() {
        if (crawlerClientMapper == null && applicationContext != null) {
            try {
                crawlerClientMapper = applicationContext.getBean(CrawlerClientMapper.class);
            } catch (BeansException e) {
                // 如果无法获取bean，返回null
                return null;
            }
        }
        return crawlerClientMapper;
    }
    
    public static CrawlerResultMapper getCrawlerResultMapper() {
        if (crawlerResultMapper == null && applicationContext != null) {
            try {
                crawlerResultMapper = applicationContext.getBean(CrawlerResultMapper.class);
            } catch (BeansException e) {
                // 如果无法获取bean，返回null
                return null;
            }
        }
        return crawlerResultMapper;
    }
    
    public static WebSocketService getWebSocketService() {
        if (webSocketService == null && applicationContext != null) {
            try {
                webSocketService = applicationContext.getBean(WebSocketService.class);
            } catch (BeansException e) {
                // 如果无法获取bean，返回null
                return null;
            }
        }
        return webSocketService;
    }
    
    public static ClientCacheService getClientCacheService() {
        if (clientCacheService == null && applicationContext != null) {
            try {
                clientCacheService = applicationContext.getBean(ClientCacheService.class);
            } catch (BeansException e) {
                // 如果无法获取bean，返回null
                return null;
            }
        }
        return clientCacheService;
    }
}