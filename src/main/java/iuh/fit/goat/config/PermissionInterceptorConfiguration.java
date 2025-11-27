package iuh.fit.goat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PermissionInterceptorConfiguration implements WebMvcConfigurer {

    @Bean
    PermissionInterceptor getPermissionInterceptor() {
        return new PermissionInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] whiteList = {
                "/api/v1/ping",                      // Endpoint kiểm tra trạng thái server
                "/api/v1/clear-cookies",             // Xóa toàn bộ cookies trên FE – không cần phân quyền
                "/",                                 // Trang gốc – public
                "/api/v1/auth/**",                   // Các endpoint xác thực (login, refresh token, logout) – phải public
                "/storage/**",                       // Tải/hiển thị file tĩnh – public
                "/api/v1/recruiters/**",             // Danh sách/Nội dung nhà tuyển dụng – public
                "/api/v1/jobs/**",                   // Danh sách/Nội dung công việc – public
                "/api/v1/skills/**",                 // Dữ liệu kỹ năng tham chiếu – public
                "/api/v1/files",                     // Upload/Download file – không yêu cầu phân quyền
                "/api/v1/applications/**",           // Ứng tuyển công việc – cho ứng viên sử dụng trực tiếp
                "/api/v1/careers/**",                // Dữ liệu ngành nghề – public
                "/api/v1/roles/**",                  // Lấy danh sách role – không cần kiểm tra permission
                "/api/v1/subscribers/**",            // Đăng ký nhận bản tin – public
                "/api/v1/dashboard/**",              // Dashboard thống kê public (nếu không yêu cầu phân quyền)
                "/api/v1/users/me/**",               // Truy vấn, thao tác với resource của chính user đang login, không cần kiểm tra permission
                "/api/v1/users/update-password",     // Cập nhật mật khẩu bằng token – cần bỏ qua interceptor
                "/api/v1/users/reset-password",      // Quên mật khẩu / reset mật khẩu – public
                "/api/v1/users/saved-jobs",          // Danh sách công việc đã lưu – cho phép FE sử dụng
                "/api/v1/users/followed-recruiters", // Danh sách NTĐ đã theo dõi – cho phép FE sử dụng
                "/api/v1/users",                     // Đăng ký hoặc truy vấn user – public
                "/api/v1/comments/**",               // Bình luận (đọc/ghi) – public
                "/api/v1/blogs/**",                  // Bài viết blog – public
                "/api/v1/email/**",                  // Gửi email/liên hệ – public

                "/v3/api-docs/**",                   // Tài liệu OpenAPI – public
                "/swagger-ui/**",                    // Swagger UI – public
                "/swagger-ui.html",                  // Trang Swagger – public

                "/api/v1/applicants/me",             // Truy vấn thông tin cá nhân của ứng viên đang đăng nhập – không cần kiểm tra permission
                "/api/v1/recruiters/me",             // Truy vấn thông tin cá nhân của NTĐ đang đăng nhập – không cần kiểm tra permission
                "/api/v1/notifications/**"           // Server sent event, nhận thông báo khi comment, reply, like blog
        };

        registry.addInterceptor(getPermissionInterceptor())
                .excludePathPatterns(whiteList);
    }
}
