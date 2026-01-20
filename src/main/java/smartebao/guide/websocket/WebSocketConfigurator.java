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

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 从握手请求中获取参数
        if (request.getQueryString() != null) {
            String queryString = request.getQueryString();
            String[] params = queryString.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    // 直接将token存储到配置中，不进行验证
                    // 因为WebSocket配置器在Spring上下文初始化前就被创建了
                    String token = keyValue[1];
                    sec.getUserProperties().put("token", token);
                    break;
                }
            }
        }
    }
}