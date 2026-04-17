package com.bank_web_app.backend.auth.controller;

import com.bank_web_app.backend.auth.dto.request.LoginRequest;
import com.bank_web_app.backend.auth.dto.request.RefreshTokenRequest;
import com.bank_web_app.backend.auth.dto.response.LoginResponse;
import com.bank_web_app.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication APIs")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(
		summary = "Login",
		description = "Authenticate user by email and password.",
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
}
