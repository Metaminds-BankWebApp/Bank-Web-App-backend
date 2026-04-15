package com.bank_web_app.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI primecoreOpenApi() {
        return new OpenAPI()
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
