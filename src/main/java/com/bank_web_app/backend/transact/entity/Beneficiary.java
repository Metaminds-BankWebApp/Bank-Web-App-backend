package com.bank_web_app.backend.transact.entity;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
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
@Table(name = "bank_customer_beneficiaries")
@Getter
@Setter
@NoArgsConstructor
public class Beneficiary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "beneficiary_id")
	private Long beneficiaryId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bank_customer_id", nullable = false)
	private BankCustomer bankCustomer;

	@Column(name = "beneficiary_account_no", nullable = false, length = 20)
	private String beneficiaryAccountNo;

	@Column(name = "nick_name", nullable = false, length = 100)
	private String nickName;

	@Column(name = "remark", nullable = false, length = 255)
	private String remark;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
