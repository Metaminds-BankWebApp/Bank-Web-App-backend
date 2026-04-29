package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.OtpRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {

	Optional<OtpRecord> findTopByTransaction_TransactionIdOrderByCreatedAtDesc(Long transactionId);

	List<OtpRecord> findAllByTransaction_TransactionIdOrderByCreatedAtDesc(Long transactionId);

	@Query(
		"""
		select count(o)
		from OtpRecord o
		where o.transaction.bankCustomer.bankCustomerId = :bankCustomerId
		  and upper(o.otpStatus) = :otpStatus
		"""
	)
	long countByBankCustomerIdAndOtpStatus(
		@Param("bankCustomerId") Long bankCustomerId,
		@Param("otpStatus") String otpStatus
	);
}
