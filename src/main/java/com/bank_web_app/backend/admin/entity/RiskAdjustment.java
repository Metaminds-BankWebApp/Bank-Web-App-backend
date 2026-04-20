package com.bank_web_app.backend.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "risk_adjustments")
@Getter
@Setter
@NoArgsConstructor
public class RiskAdjustment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "adjustment_id")
	private Long adjustmentId;

	@Column(name = "risk_level", nullable = false, unique = true, length = 20)
	private String riskLevel;

	@Column(name = "multiplier", nullable = false, precision = 4, scale = 2)
	private BigDecimal multiplier;

	@Column(name = "description", length = 255)
	private String description;

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
