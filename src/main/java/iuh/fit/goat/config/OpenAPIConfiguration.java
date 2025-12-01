package iuh.fit.goat.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfiguration {
    @Bean
    public OpenAPI myOpenAPI() {
        return new OpenAPI()
                .info(createAPIInfo())
                .servers(List.of(
                        createServer()
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private Info createAPIInfo() {
        return new Info()
                .title("GOAT API")
                .version("1.0")
                .contact(createContact())
                .description("This API exposes all endpoints (goat)")
                .license(createLicense());
    }

    private Contact createContact() {
        return new Contact()
                .email("nguyenthangdat84@gmail.com")
                .name("GOAT")
                .url("nguyenthangdat84@gmail.com");
    }

    private License createLicense() {
        return new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");
    }

    private Server createServer() {
        return new Server()
                .url("http://localhost:8080")
                .description("Server URL in Development environment");
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
