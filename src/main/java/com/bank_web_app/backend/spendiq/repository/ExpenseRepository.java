package com.bank_web_app.backend.spendiq.repository;

import com.bank_web_app.backend.spendiq.entity.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	List<Expense> findAllByUser_UserIdOrderByExpenseDateDescCreatedAtDesc(Long userId);

	List<Expense> findAllByUser_UserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
		Long userId,
		LocalDate fromDate,
		LocalDate toDate
	);

	boolean existsByTrackingSourceAndTrackingReference(String trackingSource, String trackingReference);
}
