package com.bank_web_app.backend.auth.repository;

import com.bank_web_app.backend.auth.entity.RefreshToken;
import com.bank_web_app.backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	long countByUserAndRevokedFalse(User user);
}
