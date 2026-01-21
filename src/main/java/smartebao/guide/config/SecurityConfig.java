package smartebao.guide.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // 允许访问API文档相关路径
                        .antMatchers("/doc.html").permitAll()
                        .antMatchers("/webjars/**").permitAll()
                        .antMatchers("/v3/api-docs/**").permitAll()
                        .antMatchers("/swagger-ui/**").permitAll()
                        .antMatchers("/swagger-ui.html").permitAll()
                        // 允许访问登录接口
                        .antMatchers("/api/login").permitAll()
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