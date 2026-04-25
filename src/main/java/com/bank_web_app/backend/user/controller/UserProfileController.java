package com.bank_web_app.backend.user.controller;

import com.bank_web_app.backend.user.dto.request.UserProfileUpdateRequest;
import com.bank_web_app.backend.user.dto.response.UserProfileResponse;
import com.bank_web_app.backend.user.dto.response.UserProfileUpdateResponse;
import com.bank_web_app.backend.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/profile")
@Tag(name = "User Profile", description = "Authenticated profile APIs shared across all roles.")
public class UserProfileController {

	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping
	@Operation(
		summary = "Current profile",
		description = "Return the authenticated user's role-aware profile page data.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Profile loaded successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<UserProfileResponse> getMyProfile() {
		return ResponseEntity.ok(userProfileService.getMyProfile());
	}

	@PutMapping
	@Operation(
		summary = "Update current profile",
		description = "Update the authenticated user's profile page data, including personal info, optional username change, and optional password change.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Profile updated successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Authentication required"),
			@ApiResponse(responseCode = "409", description = "Username or email already exists")
		}
	)
	public ResponseEntity<UserProfileUpdateResponse> updateMyProfile(
		@Valid @RequestBody UserProfileUpdateRequest request
	) {
		return ResponseEntity.ok(userProfileService.updateMyProfile(request));
	}
}
