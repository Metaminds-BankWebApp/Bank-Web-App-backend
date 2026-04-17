package com.bank_web_app.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI primecoreOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("Current host")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                        new io.swagger.v3.oas.models.Components().addSecuritySchemes(
                                "bearerAuth",
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .info(new Info()
                        .title("Primecore Backend API")
                        .description("API documentation for Primecore Backend services")
                        .version("v1")
                        .contact(new Contact()
                                .name("Primecore Team")
                                .email("support@primecore.local"))
                        .license(new License().name("Proprietary")));
    }
}
