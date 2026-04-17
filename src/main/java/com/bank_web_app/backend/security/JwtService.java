package com.bank_web_app.backend.security;

import com.bank_web_app.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long accessTokenExpirationMs;

	public JwtService(
		@Value("${jwt.secret}") String secret,
		@Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs
	) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMs = accessTokenExpirationMs;
	}

	public String generateAccessToken(User user) {
		Date now = new Date();
		Date expiresAt = new Date(now.getTime() + accessTokenExpirationMs);

		return Jwts.builder()
			.subject(user.getEmail())
			.claim("role", user.getRole().getRoleName())
			.claim("userId", user.getUserId())
			.issuedAt(now)
			.expiration(expiresAt)
			.signWith(signingKey)
			.compact();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
			.verifyWith(signingKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public boolean isTokenValid(String token) {
		try {
			Claims claims = parseClaims(token);
			Date expiration = claims.getExpiration();
			return expiration != null && expiration.after(new Date());
		} catch (Exception ex) {
			return false;
		}
	}

	public long getAccessTokenExpirationMs() {
		return accessTokenExpirationMs;
	}
}
