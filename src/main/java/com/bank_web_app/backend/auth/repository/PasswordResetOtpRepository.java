package com.bank_web_app.backend.auth.repository;

import com.bank_web_app.backend.auth.entity.PasswordResetOtp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

	List<PasswordResetOtp> findAllByUser_UserIdAndOtpStatusIn(Long userId, Collection<String> statuses);

	Optional<PasswordResetOtp> findTopByUser_UserIdOrderByCreatedAtDesc(Long userId);

	Optional<PasswordResetOtp> findTopByUser_UserIdAndResetTokenHashAndOtpStatusOrderByCreatedAtDesc(
		Long userId,
		String resetTokenHash,
		String otpStatus
	);
}
