package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	Optional<Transaction> findByReferenceNo(String referenceNo);

	Optional<Transaction> findByTransactionIdAndBankCustomer_BankCustomerId(Long transactionId, Long bankCustomerId);

	Optional<Transaction> findByReferenceNoAndBankCustomer_BankCustomerId(String referenceNo, Long bankCustomerId);

	List<Transaction> findAllByBankCustomer_BankCustomerIdOrderByTransactionDateDesc(Long bankCustomerId);

	boolean existsByReferenceNo(String referenceNo);

	@Query(
		"""
		select count(t)
		from Transaction t
		where t.senderAccountNo = :accountNo
		   or t.receiverAccountNo = :accountNo
		"""
	)
	long countAllByAccountNo(@Param("accountNo") String accountNo);

	@Query(
		"""
		select coalesce(sum(t.amount), 0)
		from Transaction t
		where t.senderAccountNo = :accountNo
		  and upper(t.status) = :status
		"""
	)
	BigDecimal sumSentAmountByAccountNoAndStatus(
		@Param("accountNo") String accountNo,
		@Param("status") String status
	);

	@Query(
		"""
		select coalesce(sum(t.amount), 0)
		from Transaction t
		where t.receiverAccountNo = :accountNo
		  and upper(t.status) = :status
		"""
	)
	BigDecimal sumReceivedAmountByAccountNoAndStatus(
		@Param("accountNo") String accountNo,
		@Param("status") String status
	);

	@Query(
		"""
		select count(t)
		from Transaction t
		where (t.senderAccountNo = :accountNo or t.receiverAccountNo = :accountNo)
		  and upper(t.status) = :status
		"""
	)
	long countAllByAccountNoAndStatus(
		@Param("accountNo") String accountNo,
		@Param("status") String status
	);

	@Query(
		"""
		select t
		from Transaction t
		where (t.senderAccountNo = :accountNo or t.receiverAccountNo = :accountNo)
		  and upper(t.status) = :status
		  and t.transactionDate >= :fromDate
		order by t.transactionDate asc
		"""
	)
	List<Transaction> findAllByAccountNoAndStatusFromDate(
		@Param("accountNo") String accountNo,
		@Param("status") String status,
		@Param("fromDate") LocalDateTime fromDate
	);

	@Query(
		"""
		select t
		from Transaction t
		where t.senderAccountNo = :accountNo
		   or t.receiverAccountNo = :accountNo
		order by t.transactionDate desc
		"""
	)
	List<Transaction> findRecentByAccountNo(
		@Param("accountNo") String accountNo,
		Pageable pageable
	);
}
