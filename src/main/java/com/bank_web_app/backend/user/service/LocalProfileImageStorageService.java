package com.bank_web_app.backend.user.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalProfileImageStorageService implements ProfileImageStorageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalProfileImageStorageService.class);
	private static final String PUBLIC_PATH_PREFIX = "/profile-images/";
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/jpg",
		"image/png",
		"image/webp",
		"image/gif"
	);

	private final Path storageRoot;

	public LocalProfileImageStorageService(
		@Value("${app.storage.profile-images-dir:uploads/profile-images}") String profileImagesDirectory
	) {
		this.storageRoot = Paths.get(profileImagesDirectory).toAbsolutePath().normalize();
	}

	@Override
	public String storeProfileImage(MultipartFile file, String currentImageUrl, Long userId) {
		validateFile(file);
		ensureStorageRoot();

		String storedFileName = buildStoredFileName(file, userId);
		Path target = storageRoot.resolve(storedFileName).normalize();
		if (!target.startsWith(storageRoot)) {
			throw new IllegalStateException("Resolved profile image path is outside the configured storage directory.");
		}

		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to store profile image locally.", ex);
		}

		deleteProfileImageQuietly(currentImageUrl);
		return PUBLIC_PATH_PREFIX + storedFileName;
	}

	@Override
	public void deleteProfileImage(String imageUrl) {
		String storedFileName = extractStoredFileName(imageUrl);
		if (storedFileName.isBlank()) {
			return;
		}

		Path target = storageRoot.resolve(storedFileName).normalize();
		if (!target.startsWith(storageRoot)) {
			throw new IllegalStateException("Resolved profile image path is outside the configured storage directory.");
		}

		try {
			Files.deleteIfExists(target);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to delete profile image from local storage.", ex);
		}
	}

	private void deleteProfileImageQuietly(String imageUrl) {
		try {
			deleteProfileImage(imageUrl);
		} catch (RuntimeException ex) {
			LOGGER.warn("Unable to delete previous profile image {}: {}", imageUrl, ex.getMessage());
		}
	}

	private void validateFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Profile image file is required.");
		}

		String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new IllegalArgumentException("Only JPG, PNG, WEBP, or GIF profile images are allowed.");
		}
	}

	private void ensureStorageRoot() {
		try {
			Files.createDirectories(storageRoot);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to prepare local profile image storage directory.", ex);
		}
	}

	private String buildStoredFileName(MultipartFile file, Long userId) {
		String extension = resolveExtension(file);
		String userSegment = userId == null ? "user" : ("user-" + userId);
		return userSegment + "-" + UUID.randomUUID() + extension;
	}

	private String resolveExtension(MultipartFile file) {
		String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
		return switch (contentType) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			case "image/gif" -> ".gif";
			case "image/jpeg", "image/jpg" -> ".jpg";
			default -> deriveExtensionFromOriginalFilename(file.getOriginalFilename());
		};
	}

	private String deriveExtensionFromOriginalFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return ".jpg";
		}

		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
			return ".jpg";
		}

		String extension = originalFilename.substring(lastDotIndex).trim().toLowerCase(Locale.ROOT);
		if (extension.length() > 10 || extension.contains("/") || extension.contains("\\") || extension.contains("..")) {
			return ".jpg";
		}
		return extension;
	}

	private String extractStoredFileName(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return "";
		}

		String normalized = imageUrl.trim();
		int publicPathIndex = normalized.indexOf(PUBLIC_PATH_PREFIX);
		if (publicPathIndex >= 0) {
			normalized = normalized.substring(publicPathIndex + PUBLIC_PATH_PREFIX.length());
		}

		int queryIndex = normalized.indexOf('?');
		if (queryIndex >= 0) {
			normalized = normalized.substring(0, queryIndex);
		}

		int fragmentIndex = normalized.indexOf('#');
		if (fragmentIndex >= 0) {
			normalized = normalized.substring(0, fragmentIndex);
		}

		normalized = normalized.replace('\\', '/');
		int lastSlashIndex = normalized.lastIndexOf('/');
		if (lastSlashIndex >= 0) {
			normalized = normalized.substring(lastSlashIndex + 1);
		}
		return normalized.trim();
	}
}
