package com.bank_web_app.backend.spendiq.repository;

import com.bank_web_app.backend.spendiq.entity.Expense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	List<Expense> findAllByAccount_AccountIdOrderByExpenseDateDescExpenseIdDesc(Long accountId);

	List<Expense> findAllByAccount_AccountIdAndExpenseDateBetweenOrderByExpenseDateDescExpenseIdDesc(
		Long accountId,
		LocalDate from,
		LocalDate to
	);
}
