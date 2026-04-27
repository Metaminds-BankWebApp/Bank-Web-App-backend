package com.bank_web_app.backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "UserProfileResponse", description = "Role-aware profile payload for the authenticated user.")
public record UserProfileResponse(
	@Schema(example = "42")
	Long userId,

	@Schema(example = "PUBLIC_CUSTOMER")
	String roleName,

	@Schema(example = "Public Customer")
	String roleDisplayName,

	@Schema(example = "PUBLIC CUSTOMER")
	String badgeText,

	@Schema(example = "John Doe")
	String fullName,

	@Schema(example = "JD")
	String initials,

	@Schema(example = "john.doe@primecore.bank")
	String email,

	@Schema(example = "+94771234567")
	String phone,

	@Schema(example = "johnDoePC1")
	String username,

	@Schema(example = "972346682V")
	String nic,

	@Schema(example = "1997-09-11")
	String dob,

	@Schema(example = "Colombo Central")
	String address,

	@Schema(example = "https://cdn.example.com/profile/john-doe.png", nullable = true)
	String profilePictureUrl,

	@Schema(example = "ACTIVE")
	String status,

	@Schema(example = "2026-04-22")
	String joinedDate,

	@Schema(example = "PC-00042", nullable = true)
	String customerCode,

	@Schema(example = "EMP-00042", nullable = true)
	String employeeCode,

	@Schema(example = "012345678901", nullable = true)
	String accountNumber,

	@Schema(example = "Colombo Central Branch", nullable = true)
	String branchName,

	@Schema(example = "Colombo Central", nullable = true)
	String branchLocation,

	@ArraySchema(schema = @Schema(implementation = UserProfileSummaryItemResponse.class))
	List<UserProfileSummaryItemResponse> summaryItems
) {
}
