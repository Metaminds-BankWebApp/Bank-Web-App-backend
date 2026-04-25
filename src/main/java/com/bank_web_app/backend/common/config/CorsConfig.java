package com.bank_web_app.backend.common.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private static final List<String> ALLOWED_ORIGINS = List.of("http://localhost:3000", "http://127.0.0.1:3000");
	private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry
			.addMapping("/api/**")
			.allowedOrigins(ALLOWED_ORIGINS.toArray(String[]::new))
			.allowedMethods(ALLOWED_METHODS.toArray(String[]::new))
			.allowedHeaders("*")
			.allowCredentials(true)
			.maxAge(3600);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(ALLOWED_ORIGINS);
		configuration.setAllowedMethods(ALLOWED_METHODS);
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
