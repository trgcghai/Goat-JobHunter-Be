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
                "/", "/api/v1/auth/**", "/storage/**",
                "/api/v1/recruiters/**", "/api/v1/jobs/**", "/api/v1/skills/**", "/api/v1/files",
                "/api/v1/applications/**", "/api/v1/careers/**", "/api/v1/roles/**", "/api/v1/users",
                "/api/v1/users/update-password", "/api/v1/users/reset-password",
                "/api/v1/subscribers/**", "/api/v1/dashboard/**",
                "/api/v1/users/saved-jobs", "/api/v1/users/followed-recruiters",
                "/api/v1/comments/**", "/api/v1/blogs/**", "/api/v1/email/**"
        };

        registry.addInterceptor(getPermissionInterceptor())
                .excludePathPatterns(whiteList);
    }
}
