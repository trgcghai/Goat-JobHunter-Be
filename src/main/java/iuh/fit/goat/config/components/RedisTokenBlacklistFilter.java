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

        String[] whiteList = {
                "/api/v1/ping",                       // Endpoint kiểm tra trạng thái server
                "/api/v1/clear-cookies",              // Xóa toàn bộ cookies trên FE – không cần phân quyền

                "/",                                  // Trang gốc

                "/api/v1/auth/login",                 // Đăng nhập – public
                "/api/v1/auth/register/**",           // Đăng ký tài khoản – public
                "/api/v1/auth/refresh",               // Refresh token – không kiểm tra permission
                "/api/v1/auth/verify/**",             // Xác minh email – public
                "/api/v1/auth/resend",                // Gửi lại email xác minh – public

                "/storage/**",                        // Truy cập file tĩnh – public
                "/api/v1/recruiters/**",              // Danh sách/chi tiết nhà tuyển dụng – public
                "/api/v1/jobs/**",                    // Danh sách/chi tiết công việc – public

                "/api/v1/email/**",                   // Gửi email / form liên hệ – public
                "/api/v1/blogs/**",                   // Nội dung blog – public
                "/api/v1/users/reset-password",       // Quên mật khẩu / đặt lại mật khẩu – public

                "/v3/api-docs/**",                    // Tài liệu OpenAPI – public
                "/swagger-ui/**",                     // Swagger UI – public
                "/swagger-ui.html"                    // Trang Swagger – public
        };

        String requestURI = request.getRequestURI();
        if (Arrays.stream(whiteList).anyMatch(requestURI::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

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

