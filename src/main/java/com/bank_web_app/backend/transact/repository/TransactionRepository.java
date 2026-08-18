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

// Repository for transaction persistence, history, and dashboard aggregations.
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	// Finds a transaction by unique reference number.
	Optional<Transaction> findByReferenceNo(String referenceNo);

	// Finds transaction by id scoped to a customer owner.
	Optional<Transaction> findByTransactionIdAndBankCustomer_BankCustomerId(Long transactionId, Long bankCustomerId);

	// Finds transaction by reference scoped to a customer owner.
	Optional<Transaction> findByReferenceNoAndBankCustomer_BankCustomerId(String referenceNo, Long bankCustomerId);

	// Returns all transactions for one customer, latest first.
	List<Transaction> findAllByBankCustomer_BankCustomerIdOrderByTransactionDateDesc(Long bankCustomerId);

	// Checks whether a reference number already exists.
	boolean existsByReferenceNo(String referenceNo);

	// Counts all transactions where account appears as sender or receiver.
	@Query(
		"""
		select count(t)
		from Transaction t
		where t.senderAccountNo = :accountNo
		   or t.receiverAccountNo = :accountNo
		"""
	)
	long countAllByAccountNo(@Param("accountNo") String accountNo);

	// Sums successful sent amounts from a specific account.
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

	// Sums successful received amounts into a specific account.
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

	// Counts account transactions filtered by status.
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

	// Returns account transactions from a start datetime filtered by status, oldest first.
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

	// Returns recent transactions for an account with pageable limit.
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

	// Returns all account transactions for a status, newest first.
	@Query(
		"""
		select t
		from Transaction t
		where (t.senderAccountNo = :accountNo or t.receiverAccountNo = :accountNo)
		  and upper(t.status) = :status
		order by t.transactionDate desc
		"""
	)
	List<Transaction> findAllByAccountNoAndStatusOrderByTransactionDateDesc(
		@Param("accountNo") String accountNo,
		@Param("status") String status
	);

	// Returns account transactions for a status within datetime range, oldest first.
	@Query(
		"""
		select t
		from Transaction t
		where (t.senderAccountNo = :accountNo or t.receiverAccountNo = :accountNo)
		  and upper(t.status) = :status
		  and t.transactionDate >= :fromDateTime
		  and t.transactionDate <= :toDateTime
		order by t.transactionDate asc
		"""
	)
	List<Transaction> findAllByAccountNoAndStatusBetweenDatesOrderByTransactionDateAsc(
		@Param("accountNo") String accountNo,
		@Param("status") String status,
		@Param("fromDateTime") LocalDateTime fromDateTime,
		@Param("toDateTime") LocalDateTime toDateTime
	);
}
