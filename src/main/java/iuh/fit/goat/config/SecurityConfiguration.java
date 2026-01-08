package iuh.fit.goat.config;


import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import iuh.fit.goat.config.component.AuthenticationEntryPointCustom;
import iuh.fit.goat.config.component.RedisTokenBlacklistFilter;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {
    @Value("${minhdat.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        return token -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                System.out.println(">>> JWT error: " + e.getMessage());
                throw e;
            }
        };
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityUtil.JWT_ALGORITHM.getName());
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) return List.of();

            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });

        return converter;
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return request -> {
            if (request.getCookies() != null) {
                return Arrays.stream(request.getCookies())
                        .filter(cookie -> "accessToken".equals(cookie.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }

            return null;
        };
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, AuthenticationEntryPointCustom authenticationEntryPointCustom,
            RedisTokenBlacklistFilter redisTokenBlacklistFilter
    ) throws Exception {
        String[] whiteList = {
                "/api/v1/ping",                       // Endpoint kiểm tra trạng thái server
                "/api/v1/clear-cookies",              // Xóa toàn bộ cookies trên FE – không cần phân quyền
                "/api/v1/uuid",                       // Tạo UUID cho user chưa đăng nhập – không cần phân quyền
                "/api/v1/files",                      // Upload/Download file

                "/",                                  // Trang gốc

                "/api/v1/auth/login",                 // Đăng nhập – public
                "/api/v1/auth/register/**",           // Đăng ký tài khoản – public
                "/api/v1/auth/refresh",               // Refresh token – không kiểm tra permission
                "/api/v1/auth/verify/**",             // Xác minh email – public
                "/api/v1/auth/resend",                // Gửi lại email xác minh – public

                "/storage/**",                        // Truy cập file tĩnh – public
                "/api/v1/recruiters/**",              // Danh sách/chi tiết nhà tuyển dụng – public

                "/api/v1/email/**",                   // Gửi email / form liên hệ – public
                "/api/v1/users/reset-password",       // Quên mật khẩu / đặt lại mật khẩu – public

                "/v3/api-docs/**",                    // Tài liệu OpenAPI – public
                "/swagger-ui/**",                     // Swagger UI – public
                "/swagger-ui.html",                   // Trang Swagger – public

                "/api/v1/ai/**",                      // Role cũng có thể dùng chat

                "/actuator",
                "/actuator/health",                   // Actuator để kiểm tra sức khỏe ứng dụng – public
                "/actuator/health/**"                 // Actuator để kiểm tra sức khỏe ứng dụng – public
        };


        http
                .csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests( request ->
                        request.requestMatchers(whiteList).permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/reviews").hasRole("SUPER_ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/v1/recruiters/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/companies/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/careers/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/roles/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/users/**").permitAll()
                                .requestMatchers(HttpMethod.PUT, "/api/v1/users/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/blogs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/comments/**").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                                .bearerTokenResolver(bearerTokenResolver())
                                .authenticationEntryPoint(authenticationEntryPointCustom)
                )
                .addFilterBefore(redisTokenBlacklistFilter, BearerTokenAuthenticationFilter.class)
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )
                .formLogin(f -> f.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}
