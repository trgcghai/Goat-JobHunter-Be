package iuh.fit.goat.config.components;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class RedisTokenBlacklistFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTokenBlacklistFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        if(request.getCookies() != null) {
            String token = Arrays.stream(request.getCookies())
                    .filter(c -> c.getName().equalsIgnoreCase("accessToken"))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);

            if (token != null && Boolean.TRUE.equals(this.redisTemplate.hasKey("blacklist:" + token))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token has been revoked");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

