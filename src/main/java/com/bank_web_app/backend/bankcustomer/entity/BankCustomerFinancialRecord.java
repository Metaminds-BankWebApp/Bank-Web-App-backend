package com.bank_web_app.backend.bankcustomer.entity;

import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
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
@Table(name = "bank_customer_financial_records")
@Getter
@Setter
@NoArgsConstructor
public class BankCustomerFinancialRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "bank_record_id")
	private Long bankRecordId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_customer_id", nullable = false)
	private BankCustomer bankCustomer;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "verified_by_officer_id", nullable = false)
	private BankOfficer verifiedByOfficer;

	@Column(name = "data_source", nullable = false, length = 30)
	private String dataSource = "MANUAL";

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		if (dataSource == null || dataSource.isBlank()) {
			dataSource = "MANUAL";
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
