package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.OtpRecord;
import java.time.LocalDateTime;
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

	// Finds OTPs that have expired while their transfer is still awaiting verification.
	@Query(
		"""
		select o
		from OtpRecord o
		where upper(o.transaction.status) = :transactionStatus
		  and upper(o.otpStatus) = :otpStatus
		  and o.expiresAt <= :now
		"""
	)
	List<OtpRecord> findExpiredPendingOtps(
		@Param("transactionStatus") String transactionStatus,
		@Param("otpStatus") String otpStatus,
		@Param("now") LocalDateTime now
	);

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
