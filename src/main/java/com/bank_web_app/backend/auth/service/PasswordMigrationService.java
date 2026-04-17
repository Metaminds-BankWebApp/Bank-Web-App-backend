package com.bank_web_app.backend.auth.service;

import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordMigrationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public PasswordMigrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void migratePlaintextPasswordsToBcrypt() {
		List<User> users = userRepository.findAll();
		for (User user : users) {
			String storedPassword = user.getPasswordHash();
			if (storedPassword == null || storedPassword.isBlank()) {
				continue;
			}

			if (isBcryptHash(storedPassword)) {
				continue;
			}

			user.setPasswordHash(passwordEncoder.encode(storedPassword));
			userRepository.save(user);
		}
	}

	private boolean isBcryptHash(String value) {
		return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
	}
}
