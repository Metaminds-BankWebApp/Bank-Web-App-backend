package com.bank_web_app.backend.notification.service;

import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationCurrentUserService {

	private final UserRepository userRepository;

	public NotificationCurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User resolveRequiredUser() {
		return resolveOptionalUser()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required."));
	}

	public Optional<User> resolveOptionalUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			return Optional.empty();
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		return userRepository
			.findByEmailIgnoreCase(normalizedPrincipal)
			.or(() -> userRepository.findByUsernameIgnoreCase(principal));
	}
}
