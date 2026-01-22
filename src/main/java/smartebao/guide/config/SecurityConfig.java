package smartebao.guide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // 允许访问API文档相关路径
                        .requestMatchers(new AntPathRequestMatcher("/doc.html")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                        // 允许访问登录接口
                        .requestMatchers(new AntPathRequestMatcher("/api/register")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/login")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/health/**")).permitAll()
                        // 重要：允许WebSocket连接路径无需认证（JWT验证将在WebSocket内部进行）
                        .requestMatchers(new AntPathRequestMatcher("/ws")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/ws/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/smarteCrawler/ws")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/smarteCrawler/ws/**")).permitAll()
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 无状态，不创建session
                )
                .csrf(csrf -> csrf.disable()); // 禁用csrf，因为是API服务

        return http.build();
    }
}