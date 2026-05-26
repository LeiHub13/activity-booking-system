package com.example.activitybookingsystem.filter;

import com.example.activitybookingsystem.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;

    public JwtAuthenticationFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = JwtUtil.parseToken(token);
            log.info("claim: {}", claims.toString());
            log.info("token: {}", token);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            // Redis 里存在这条 token 记录，才认为当前登录态仍然有效。
            String redisValue = stringRedisTemplate.opsForValue().get("login:token:" + token);

            if (username != null && role != null && redisValue != null) {
                // 把 JWT 里的角色装进 Spring Security，上层接口才能走 hasRole 规则。
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception ex) {
            // token 解析失败或已失效时，清空上下文，后续请求会按未登录处理。
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
