package com.bank_web_app.backend.security;

import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String authHeader = request.getHeader(AUTHORIZATION_HEADER);
		if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(BEARER_PREFIX.length()).trim();
		if (token.isBlank() || !jwtService.isTokenValid(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		Claims claims = jwtService.parseClaims(token);
		String subjectEmail = claims.getSubject();
		if (subjectEmail == null || subjectEmail.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		String normalizedEmail = subjectEmail.trim().toLowerCase(Locale.ROOT);
		User user = userRepository.findByEmail(normalizedEmail).orElse(null);
		if (user == null || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
			filterChain.doFilter(request, response);
			return;
		}

		String role = claims.get("role", String.class);
		if (role == null || role.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			user.getEmail(),
			null,
			List.of(new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase(Locale.ROOT)))
		);
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}
}
