package com.bank_web_app.backend.bankofficer.entity;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "officer_work_queue_cases", uniqueConstraints = @UniqueConstraint(columnNames = {"bank_customer_id", "case_type"}))
@Getter @Setter
public class OfficerWorkQueueCase {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "work_queue_case_id") private Long workQueueCaseId;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bank_customer_id", nullable = false) private BankCustomer bankCustomer;
	@Column(name = "case_type", nullable = false, length = 30) private String caseType;
	@Column(name = "status", nullable = false, length = 20) private String status;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "updated_by_officer_id", nullable = false) private BankOfficer updatedByOfficer;
	@Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
	@PrePersist void created() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
	@PreUpdate void updated() { updatedAt = LocalDateTime.now(); }
}
