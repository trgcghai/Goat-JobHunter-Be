package iuh.fit.goat.config.component;

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
                //              Các endpoint về tài nguyên tĩnh như swagger có thể public
                "/storage/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",

//              Các endpoint kiểm tra sức khỏe của server có thể public để các công cụ giám sát có thể truy cập mà không cần xác thực
                "/actuator",
                "/actuator/health",
                "/actuator/health/**",

//              Ai cũng có thể dùng chat
                "/api/v1/ai/**",

//              Số lượng ứng tuyển của công việc, có thể public để hiển thị trên trang chi tiết công việc
                "/api/v1/applications/count",

//              Các endpoint về auth để người dùng có thể đăng nhập, đăng ký, làm mới token mà không cần phải xác thực trước
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/register/**",
                "/api/v1/auth/verify",
                "/api/v1/auth/resend",

//              Các endpoint về blog để người dùng có thể xem danh sách bài viết, chi tiết bài viết, bài viết theo tag, nhưng vẫn cho phép người dùng có thể tạo/sửa/xóa bài viết của chính mình
                "/api/v1/blogs/{id}",
                "/api/v1/blogs/available",
                "/api/v1/blogs/tags",

//              Dữ liệu ngành nghề phải public
                "/api/v1/careers/**",

//              Thông tin danh sách công ty và chi tiết công ty có thể public
                "/api/v1/companies/{id}",
                "/api/v1/companies/slug/{name}",
                "/api/v1/companies/{id}/group-addresses",
                "/api/v1/companies/available",
                "/api/v1/companies/{companyId}/jobs/skills",
                "/api/v1/companies/{companyId}/jobs",
                "/api/v1/companies/name",

//              Thông tin danh sách công việc và chi tiết công việc có thể public
                "/api/v1/jobs/related",
                "/api/v1/jobs/{id}",
                "/api/v1/jobs/available",
                "/api/v1/jobs/companies/count",
                "/api/v1/jobs/count-applications",

//              Endpoint kiểm tra trạng thái server
                "/",
                "/ping",
                "/clear-cookies",
                "/uuid",

//              Các endpoint đánh giá để người dùng có thể thao tác với đánh giá của chính mình, nhưng vẫn cho phép mọi người xem danh sách đánh giá của công ty và đánh giá gần nhất
                "/api/v1/reviews/companies/{name}",
                "/api/v1/reviews/latest",
                "/api/v1/reviews/count",
                "/api/v1/reviews/companies/count",
                "/api/v1/reviews/companies/ratings/average",
                "/api/v1/reviews/companies/{companyId}/ratings/summary",
                "/api/v1/reviews/companies/{companyId}/recommendation-rate",

//              Các endpoint về skill để người dùng có thể xem danh sách kỹ năng và chi tiết kỹ năng
                "/api/v1/skills/{id}",
                "/api/v1/skills/all",

//              Các endpoint về user để người dùng có thể thao tác với thông tin của chính mình, nhưng vẫn cho phép mọi người xem thông tin cơ bản của user khác (không bao gồm thông tin nhạy cảm như email, password, vai trò)
                "/api/v1/users/{id}",
                "/api/v1/users/search",
                "/api/v1/users/reset-password",

//              Các endpoint về role để người dùng có thể xem danh sách vai trò và chi tiết vai trò
                "/api/v1/roles/{id}",
                "/api/v1/roles",
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

