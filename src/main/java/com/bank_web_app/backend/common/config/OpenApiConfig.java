package com.bank_web_app.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI primecoreOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("Current host")))
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
                        .description("API documentation for Primecore Backend services.\n\nSwagger role testing:\n- Use /api/auth/login to get JWT for a specific user role.\n- Click Authorize and paste: Bearer <access_token>.\n- Each endpoint description includes Required role.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Primecore Team")
                                .email("support@primecore.local"))
                        .license(new License().name("Proprietary")));
    }

        @Bean
        public OpenApiCustomizer roleAwareSecurityCustomizer() {
                return openApi -> {
                        if (openApi.getPaths() == null) {
                                return;
                        }

                        openApi.getPaths().forEach((path, pathItem) -> {
                                String requiredRole = resolveRequiredRole(path);

                                pathItem.readOperations().forEach(operation -> {
                                        if (path.startsWith("/api/auth")) {
                                                operation.setSecurity(new ArrayList<>());
                                                appendRoleNote(operation, "PUBLIC (no bearer token required)");
                                                return;
                                        }

                                        operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                                        appendRoleNote(operation, requiredRole == null ? "Authenticated user" : requiredRole);

                                        Map<String, Object> extensions = operation.getExtensions();
                                        if (extensions == null) {
                                                extensions = new LinkedHashMap<>();
                                                operation.setExtensions(extensions);
                                        }
                                        extensions.put("x-required-role", requiredRole == null ? "AUTHENTICATED" : requiredRole);
                                });
                        });
                };
        }

        private static void appendRoleNote(io.swagger.v3.oas.models.Operation operation, String roleLabel) {
                String roleLine = "Required role: " + roleLabel;
                String description = operation.getDescription();
                if (description == null || description.isBlank()) {
                        operation.setDescription(roleLine);
                        return;
                }

                String normalizedDescription = description.toLowerCase(Locale.ROOT);
                if (!normalizedDescription.contains("required role:")) {
                        operation.setDescription(description + "\n\n" + roleLine);
                }
        }

        private static String resolveRequiredRole(String path) {
                if (path.startsWith("/api/admin")) {
                        return "ADMIN";
                }
                if (path.startsWith("/api/bank-officers")) {
                        return "BANK_OFFICER";
                }
                if (path.startsWith("/api/public-customers")) {
                        return "PUBLIC_CUSTOMER";
                }
                if (path.startsWith("/api/bank-customers")) {
                        return "BANK_CUSTOMER";
                }
                return null;
        }
}
