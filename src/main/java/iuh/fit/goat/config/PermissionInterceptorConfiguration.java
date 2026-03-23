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
//              Các endpoint về tài nguyên tĩnh như swagger có thể public
                "/storage/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",

//              Ai cũng có thể dùng chat
                "/api/v1/ai/**",

//              Số lượng ứng tuyển của công việc, có thể public để hiển thị trên trang chi tiết công việc
                "/api/v1/applications/count",

//              Các endpoint xác thực phải public
                "/api/v1/auth/**",

//              Các endpoint về blog để người dùng có thể xem danh sách bài viết, chi tiết bài viết, bài viết theo tag, nhưng vẫn cho phép người dùng có thể tạo/sửa/xóa bài viết của chính mình
                "/api/v1/blogs",
                "/api/v1/blogs/{id}",
                "/api/v1/blogs/available",
                "/api/v1/blogs/tags",

//              Các endpoint về reaction để người dùng có thể xem danh sách reaction của bài viết, nhưng vẫn cho phép người dùng có thể tạo/xóa reaction của chính mình
                "/api/v1/reactions/**",

//              Dữ liệu ngành nghề phải public
                "/api/v1/careers/**",

//              Các endpoint về chatroom để người dùng có thể xem danh sách phòng chat của chính mình, chi tiết phòng chat của chính mình
                "/api/v1/chatrooms/**",

//              Các endpoint về comment để người dùng có thể xem danh sách comment của bài viết, chi tiết comment, nhưng vẫn cho phép người dùng có thể tạo/sửa/xóa comment của chính mình
                "/api/v1/comments/**",

//              Thông tin danh sách công ty và chi tiết công ty có thể public
                "/api/v1/companies/{id}",
                "/api/v1/companies/slug/{name}",
                "/api/v1/companies/{id}/group-addresses",
                "/api/v1/companies/available",
                "/api/v1/companies/{companyId}/jobs/skills",
                "/api/v1/companies/{companyId}/jobs",
                "/api/v1/companies/name",

//               Thông tin danh sách công việc và chi tiết công việc có thể public
                "/api/v1/jobs/{id}",
                "/api/v1/jobs/available",
                "/api/v1/jobs/companies/count",
                "/api/v1/jobs/count-applications",

//              Server sent event, nhận thông báo khi comment, reply, like blog
                "/api/v1/notifications/**",

//              Endpoint kiểm tra trạng thái server
                "/",
                "/ping",
                "/clear-cookies",
                "/uuid",

//              Các endpoint resumes để người dùng có thể thao tác với resume của chính mình
                "/api/v1/resumes/**",

//               Các endpoint đánh giá để người dùng có thể thao tác với đánh giá của chính mình
                "/api/v1/evaluations/**",

//              Các endpoint đánh giá để người dùng có thể thao tác với đánh giá của chính mình, nhưng vẫn cho phép mọi người xem danh sách đánh giá của công ty và đánh giá gần nhất
                "/api/v1/reviews",
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

//              Các endpoint về file để người dùng có thể tải lên hoặc tải xuống file
                "/api/v1/files/**",

//              Đăng ký nhận việc làm mới qua email
                "/api/v1/subscribers/**",

//              Báo cáo bài viết hoặc comment
                "/api/v1/tickets/**",

//              Các endpoint về user để người dùng có thể thao tác với thông tin của chính mình, nhưng vẫn cho phép mọi người xem thông tin cơ bản của user khác (không bao gồm thông tin nhạy cảm như email, password, vai trò)
                "/api/v1/users/{id}",
                "/api/v1/users/search",
                "/api/v1/users/me/**",
                "/api/v1/users/update-password",
                "/api/v1/users/reset-password",

//              Các endpoint về role để người dùng có thể xem danh sách vai trò và chi tiết vai trò
                "/api/v1/roles/{id}",
                "/api/v1/roles",
        };

        registry.addInterceptor(getPermissionInterceptor())
                .excludePathPatterns(whiteList);
    }
}
