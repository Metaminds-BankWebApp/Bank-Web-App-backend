package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.user.dto.request.UserProfileUpdateRequest;
import com.bank_web_app.backend.user.dto.response.UserProfileResponse;
import com.bank_web_app.backend.user.dto.response.UserProfileUpdateResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

	// Loads the authenticated user's profile page data.
	UserProfileResponse getMyProfile();

	// Updates profile details, username, or password for the authenticated user.
	UserProfileUpdateResponse updateMyProfile(UserProfileUpdateRequest request);

	// Uploads and saves a new profile image for the authenticated user.
	UserProfileUpdateResponse updateMyProfileImage(MultipartFile file);

	// Removes the authenticated user's saved profile image.
	UserProfileUpdateResponse removeMyProfileImage();
}
