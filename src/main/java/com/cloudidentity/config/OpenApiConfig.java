package com.cloudidentity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3.0 configuration.
 * Accessible at /swagger-ui/index.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI cloudIdentityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud Identity Behavior Fingerprinting System API")
                        .description("Production-grade REST API for monitoring AWS IAM identities, " +
                                "detecting behavioral anomalies, and calculating risk scores. " +
                                "Authenticate via POST /api/v1/auth/login and use the returned JWT as Bearer token.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cloud Security Team")
                                .email("security@cloudidentity.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT token obtained from /api/v1/auth/login")));
    }
}
