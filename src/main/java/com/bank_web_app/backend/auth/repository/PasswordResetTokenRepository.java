package com.bank_web_app.backend.auth.repository;

import com.bank_web_app.backend.auth.entity.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	List<PasswordResetToken> findAllByUser_UserIdAndConsumedAtIsNullOrderByCreatedAtDesc(Long userId);

	Optional<PasswordResetToken> findByResetTokenHashAndConsumedAtIsNull(String resetTokenHash);

	Optional<PasswordResetToken> findByResetTokenHash(String resetTokenHash);

	long deleteByUser_UserId(Long userId);
}
