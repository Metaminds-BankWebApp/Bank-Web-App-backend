package com.bank_web_app.backend.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorageService {

	// Stores a new profile image and returns the public image URL.
	String storeProfileImage(MultipartFile file, String currentImageUrl, Long userId);

	// Deletes the stored profile image linked to the given URL.
	void deleteProfileImage(String imageUrl);
}
