package com.bank_web_app.backend.user.service;

import com.bank_web_app.backend.user.dto.request.UserProfileUpdateRequest;
import com.bank_web_app.backend.user.dto.response.UserProfileResponse;
import com.bank_web_app.backend.user.dto.response.UserProfileUpdateResponse;

public interface UserProfileService {

	UserProfileResponse getMyProfile();

	UserProfileUpdateResponse updateMyProfile(UserProfileUpdateRequest request);
}
