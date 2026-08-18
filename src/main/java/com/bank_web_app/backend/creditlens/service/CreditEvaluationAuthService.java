package com.bank_web_app.backend.creditlens.service;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditEvaluationAuthService {

	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final BankOfficerRepository bankOfficerRepository;
	private final UserRepository userRepository;

	// Wires repositories needed to resolve the authenticated CreditLens user.
	public CreditEvaluationAuthService(
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		BankCustomerRepository bankCustomerRepository,
		BankOfficerRepository bankOfficerRepository,
		UserRepository userRepository
	) {
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.bankOfficerRepository = bankOfficerRepository;
		this.userRepository = userRepository;
	}

	// Returns the logged-in public customer profile or blocks non-public users.
	PublicCustomerProfile resolveLoggedInPublicCustomerProfile() {
		User user = resolveAuthenticatedUser("Public customer authentication is required.");
		return publicCustomerProfileRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a public customer."));
	}

	// Returns the logged-in bank customer after verifying the bank-customer role.
	BankCustomer resolveLoggedInBankCustomer() {
		User user = resolveAuthenticatedUser("Bank customer authentication is required.");
		String roleName = user.getRole() == null || user.getRole().getRoleName() == null
			? ""
			: user.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);
		if (!"BANK_CUSTOMER".equals(roleName)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a bank customer.");
		}
		return bankCustomerRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bank customer profile was not found for logged-in user."));
	}

	// Returns the bank officer profile for the logged-in officer user.
	BankOfficer resolveLoggedInBankOfficer() {
		User user = resolveAuthenticatedUser("Bank officer authentication is required.");
		return bankOfficerRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a bank officer."));
	}

	// Resolves a bank customer for a logged-in officer. All BANK_OFFICER users may access bank customers.
	BankCustomer resolveOwnedBankCustomer(Long bankCustomerId, BankOfficer officer) {
		BankCustomer bankCustomer = bankCustomerRepository
			.findById(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Bank customer not found."));
		return bankCustomer;
	}

	// Reads Spring Security authentication and loads the matching user record.
	private User resolveAuthenticatedUser(String unauthenticatedMessage) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, unauthenticatedMessage);
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		return userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));
	}
}
