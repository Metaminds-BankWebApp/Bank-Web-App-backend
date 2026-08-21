package com.bank_web_app.backend.common.config;

import com.bank_web_app.backend.admin.audit.SystemAuditLoggingInterceptor;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
	private static final List<String> EXPOSED_HEADERS = List.of("Content-Disposition");
	private final List<String> allowedOrigins;
	private final SystemAuditLoggingInterceptor systemAuditLoggingInterceptor;

	public CorsConfig(
		SystemAuditLoggingInterceptor systemAuditLoggingInterceptor,
		@Value("${app.cors.allowed-origins}") String configuredAllowedOrigins
	) {
		this.systemAuditLoggingInterceptor = systemAuditLoggingInterceptor;
		this.allowedOrigins = Arrays.stream(configuredAllowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList();
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry
			.addMapping("/api/**")
			.allowedOrigins(allowedOrigins.toArray(String[]::new))
			.allowedMethods(ALLOWED_METHODS.toArray(String[]::new))
			.allowedHeaders("*")
			.exposedHeaders(EXPOSED_HEADERS.toArray(String[]::new))
			.allowCredentials(true)
			.maxAge(3600);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(systemAuditLoggingInterceptor).addPathPatterns("/api/**");
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(ALLOWED_METHODS);
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(EXPOSED_HEADERS);
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
