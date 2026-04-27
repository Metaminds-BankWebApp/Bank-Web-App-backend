package com.bank_web_app.backend.admin.controller;

import com.bank_web_app.backend.admin.dto.response.AdminUserManagementUserResponse;
import com.bank_web_app.backend.admin.service.AdminUserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin User Management", description = "Admin user-management endpoints for BANK and PUBLIC customers.")
public class AdminUserManagementController {

	private final AdminUserManagementService adminUserManagementService;

	public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
		this.adminUserManagementService = adminUserManagementService;
	}

	@GetMapping
	@Operation(
		summary = "Get admin user-management list",
		description = "Returns BANK and PUBLIC customer users for the admin user-management module.",
		responses = {
			@ApiResponse(responseCode = "200", description = "User list loaded"),
			@ApiResponse(responseCode = "400", description = "Invalid filter parameters")
		}
	)
	public ResponseEntity<List<AdminUserManagementUserResponse>> getUsers(
		@RequestParam(defaultValue = "ALL") String customerType,
		@RequestParam(required = false) String search
	) {
		return ResponseEntity.ok(adminUserManagementService.getUsers(customerType, search));
	}

	@PatchMapping("/{userId}/status")
	@Operation(
		summary = "Update managed user status",
		description = "Updates status of a BANK or PUBLIC customer user.",
		responses = {
			@ApiResponse(responseCode = "200", description = "User status updated"),
			@ApiResponse(responseCode = "400", description = "Invalid status or user role"),
			@ApiResponse(responseCode = "404", description = "User not found")
		}
	)
	public ResponseEntity<AdminUserManagementUserResponse> updateUserStatus(
		@PathVariable Long userId,
		@RequestParam String status
	) {
		return ResponseEntity.ok(adminUserManagementService.updateUserStatus(userId, status));
	}
}
