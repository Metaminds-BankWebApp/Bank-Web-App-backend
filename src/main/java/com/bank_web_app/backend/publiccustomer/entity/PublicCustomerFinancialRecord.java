package com.bank_web_app.backend.publiccustomer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "public_customer_financial_records")
@Getter
@Setter
@NoArgsConstructor
public class PublicCustomerFinancialRecord {

	// Primary key of financial snapshot record.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "record_id")
	private Long recordId;

	// Owning public-customer profile for this record.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "public_customer_id", nullable = false)
	private PublicCustomerProfile publicCustomer;

	// Record status marker (e.g., CURRENT/ARCHIVED).
	@Column(name = "record_status", nullable = false, length = 20)
	private String recordStatus = "CURRENT";

	// Record creation timestamp.
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	// Record update timestamp.
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
