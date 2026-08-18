package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.OtpRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Repository for OTP log persistence and OTP analytics lookups.
public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {

	// Returns the latest OTP record for a given transaction.
	Optional<OtpRecord> findTopByTransaction_TransactionIdOrderByCreatedAtDesc(Long transactionId);

	// Returns full OTP history for a given transaction, newest first.
	List<OtpRecord> findAllByTransaction_TransactionIdOrderByCreatedAtDesc(Long transactionId);

	// Counts OTP logs by customer and OTP status value.
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
