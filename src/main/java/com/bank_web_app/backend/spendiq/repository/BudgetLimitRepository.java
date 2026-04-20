package com.bank_web_app.backend.spendiq.repository;

import com.bank_web_app.backend.spendiq.entity.BudgetLimit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {

	Optional<BudgetLimit> findByUser_UserIdAndCategory_CategoryIdAndMonthAndYear(
		Long userId,
		Long categoryId,
		Integer month,
		Integer year
	);

	List<BudgetLimit> findAllByUser_UserIdAndMonthAndYearOrderByCreatedAtDesc(Long userId, Integer month, Integer year);

	List<BudgetLimit> findAllByUser_UserIdOrderByYearDescMonthDescCreatedAtDesc(Long userId);
}