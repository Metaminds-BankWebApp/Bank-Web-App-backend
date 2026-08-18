package com.bank_web_app.backend.publiccustomer.entity;

import com.bank_web_app.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "public_customers")
@Getter
@Setter
@NoArgsConstructor
public class PublicCustomerProfile {

	// Primary key of public-customer profile.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "public_customer_id")
	private Long publicCustomerId;

	// Linked user account for this public-customer profile.
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	// Human-readable customer code identifier.
	@Column(name = "customer_code", nullable = false, unique = true, length = 50)
	private String customerCode;

	// Profile creation timestamp.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Profile last update timestamp.
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// Initializes audit timestamps on first persist.
	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	// Refreshes update timestamp on each modification.
	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
