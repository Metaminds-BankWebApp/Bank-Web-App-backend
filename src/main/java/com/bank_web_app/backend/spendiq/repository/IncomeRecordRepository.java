package com.bank_web_app.backend.spendiq.repository;

import com.bank_web_app.backend.spendiq.entity.IncomeRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeRecordRepository extends JpaRepository<IncomeRecord, Long> {

	List<IncomeRecord> findAllByUser_UserIdOrderByIncomeDateDescCreatedAtDesc(Long userId);

	List<IncomeRecord> findAllByUser_UserIdAndIncomeDateBetweenOrderByIncomeDateDescCreatedAtDesc(
		Long userId,
		LocalDate fromDate,
		LocalDate toDate
	);
}