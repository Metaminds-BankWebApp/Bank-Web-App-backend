package com.bank_web_app.backend.user.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorageService {

	String storeProfileImage(MultipartFile file, String currentImageUrl, Long userId);

	void deleteProfileImage(String imageUrl);
}
