package smartebao.guide.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import smartebao.guide.utils.JwtUtil;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

@Component
public class WebSocketConfigurator extends Configurator {

    private static JwtUtil jwtUtil;

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 确保Spring Bean被注入
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
        
        // 从握手请求中获取参数
        if (request.getQueryString() != null) {
            String queryString = request.getQueryString();
            String[] params = queryString.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    // 验证JWT令牌
                    String token = keyValue[1];
                    if (jwtUtil != null && !jwtUtil.validateToken(token)) {
                        throw new RuntimeException("Invalid token");
                    }
                    
                    // 将token存储到配置中，以便在WebSocket处理器中使用
                    sec.getUserProperties().put("token", token);
                    break;
                }
            }
        }
    }

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        WebSocketConfigurator.jwtUtil = jwtUtil;
    }
}