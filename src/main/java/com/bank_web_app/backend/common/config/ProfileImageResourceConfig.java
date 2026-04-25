package com.bank_web_app.backend.common.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProfileImageResourceConfig implements WebMvcConfigurer {

	private final String profileImagesDirectory;

	public ProfileImageResourceConfig(
		@Value("${app.storage.profile-images-dir:uploads/profile-images}") String profileImagesDirectory
	) {
		this.profileImagesDirectory = profileImagesDirectory;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String resourceLocation = Paths.get(profileImagesDirectory).toAbsolutePath().normalize().toUri().toString();
		if (!resourceLocation.endsWith("/")) {
			resourceLocation = resourceLocation + "/";
		}
		registry
			.addResourceHandler("/profile-images/**")
			.addResourceLocations(resourceLocation);
	}
}
