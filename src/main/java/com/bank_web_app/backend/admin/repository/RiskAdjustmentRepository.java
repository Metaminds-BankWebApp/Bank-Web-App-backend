package com.bank_web_app.backend.admin.repository;

import com.bank_web_app.backend.admin.entity.RiskAdjustment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAdjustmentRepository extends JpaRepository<RiskAdjustment, Long> {

	List<RiskAdjustment> findAllByOrderByRiskLevelAsc();

	Optional<RiskAdjustment> findByRiskLevel(String riskLevel);
}
