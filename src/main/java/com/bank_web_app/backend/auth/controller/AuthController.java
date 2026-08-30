package com.bank_web_app.backend.auth.controller;

import com.bank_web_app.backend.auth.dto.request.ForgotPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.LoginRequest;
import com.bank_web_app.backend.auth.dto.request.OfficerActivationTokenRequest;
import com.bank_web_app.backend.auth.dto.request.RefreshTokenRequest;
import com.bank_web_app.backend.auth.dto.request.ResetPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.bank_web_app.backend.auth.dto.response.AuthActionResponse;
import com.bank_web_app.backend.auth.dto.response.AuthMeResponse;
import com.bank_web_app.backend.auth.dto.response.LoginResponse;
import com.bank_web_app.backend.auth.dto.response.OfficerActivationResponse;
import com.bank_web_app.backend.auth.service.AuthService;
import com.bank_web_app.backend.publiccustomer.service.PublicCustomerService;
import com.bank_web_app.backend.user.dto.request.UserRegistrationStepOneRequest;
import com.bank_web_app.backend.user.dto.response.UserRegistrationStepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication APIs")
public class AuthController {

	private final AuthService authService;
	private final PublicCustomerService publicCustomerService;

	public AuthController(AuthService authService, PublicCustomerService publicCustomerService) {
		this.authService = authService;
		this.publicCustomerService = publicCustomerService;
	}

	@PostMapping("/register")
	@Operation(
		summary = "Register public customer",
		description = "Creates a public customer account without requiring authentication.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Public customer created successfully"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "409", description = "Conflict: NIC, email, or username already in use")
		}
	)
	public ResponseEntity<UserRegistrationStepResponse> register(@Valid @RequestBody UserRegistrationStepOneRequest request) {
		return ResponseEntity.ok(publicCustomerService.register(request));
	}

	@PostMapping("/login")
	@Operation(
		summary = "Login",
		description = "Authenticate user by email address or username and password.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Login successful"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Invalid credentials"),
			@ApiResponse(responseCode = "403", description = "Inactive account")
		}
	)
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	@Operation(
		summary = "Refresh access token",
		description = "Rotate refresh token and issue a new access token.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Token refresh successful"),
			@ApiResponse(responseCode = "400", description = "Validation failed"),
			@ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
			@ApiResponse(responseCode = "403", description = "Inactive account")
		}
	)
	public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@PostMapping("/logout")
	@Operation(
		summary = "Logout",
		description = "Revoke current refresh token explicitly.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Logout successful"),
			@ApiResponse(responseCode = "400", description = "Validation failed")
		}
	)
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/forgot-password")
	@Operation(summary = "Send password reset OTP", description = "Looks up an active account by email address or username and sends a 6-digit code to its registered email address.")
	public ResponseEntity<AuthActionResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		return ResponseEntity.ok(authService.forgotPassword(request));
	}

	@PostMapping("/verify-otp")
	@Operation(summary = "Verify password reset OTP", description = "Validates a 6-digit password-reset OTP and creates a one-time reset session.")
	public ResponseEntity<AuthActionResponse> verifyOtp(@Valid @RequestBody VerifyPasswordResetOtpRequest request) {
		return ResponseEntity.ok(authService.verifyPasswordResetOtp(request));
	}

	@PostMapping("/reset-password")
	@Operation(summary = "Reset password", description = "Sets a new password with a verified, short-lived reset session and revokes existing refresh-token sessions.")
	public ResponseEntity<AuthActionResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		return ResponseEntity.ok(authService.resetPassword(request));
	}

	@PostMapping("/officer-activation/status")
	@Operation(summary = "Inspect officer activation link", description = "Returns whether an officer activation link is valid, expired, completed, or eligible for self-service resend.")
	public ResponseEntity<OfficerActivationResponse> inspectOfficerActivation(
		@Valid @RequestBody OfficerActivationTokenRequest request
	) {
		return ResponseEntity.ok(authService.inspectOfficerActivation(request));
	}

	@PostMapping("/officer-activation/resend")
	@Operation(summary = "Resend officer activation link", description = "Sends a replacement three-day activation link to a pending officer, up to three resend requests.")
	public ResponseEntity<OfficerActivationResponse> resendOfficerActivation(
		@Valid @RequestBody OfficerActivationTokenRequest request
	) {
		return ResponseEntity.ok(authService.resendOfficerActivation(request));
	}

	@GetMapping("/me")
	@Operation(
		summary = "Current authenticated user",
		description = "Resolve the active signed-in user and their domain ownership IDs.",
		responses = {
			@ApiResponse(responseCode = "200", description = "Current user resolved successfully"),
			@ApiResponse(responseCode = "401", description = "Authentication required")
		}
	)
	public ResponseEntity<AuthMeResponse> me() {
		return ResponseEntity.ok(authService.me());
	}
}
