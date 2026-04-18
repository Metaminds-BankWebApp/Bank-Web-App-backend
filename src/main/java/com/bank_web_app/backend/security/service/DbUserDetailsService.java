package com.bank_web_app.backend.security.service;

import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbUserDetailsService implements UserDetailsService {

	private static final String STATUS_INACTIVE = "INACTIVE";

	private final UserRepository userRepository;

	public DbUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		String normalizedEmail = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
		if (normalizedEmail.isBlank()) {
			throw new UsernameNotFoundException("Email is required.");
		}

		User user = userRepository
			.findByEmail(normalizedEmail)
			.orElseThrow(() -> new UsernameNotFoundException("User not found."));

		String roleName = user.getRole() == null ? "USER" : user.getRole().getRoleName();
		String authority = "ROLE_" + roleName.trim().toUpperCase(Locale.ROOT);
		boolean active = !STATUS_INACTIVE.equalsIgnoreCase(user.getStatus());

		return org.springframework.security.core.userdetails.User
			.withUsername(user.getEmail())
			.password(user.getPasswordHash())
			.authorities(List.of(new SimpleGrantedAuthority(authority)))
			.disabled(!active)
			.build();
	}
}
