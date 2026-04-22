package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	Optional<Transaction> findByReferenceNo(String referenceNo);

	Optional<Transaction> findByTransactionIdAndBankCustomer_BankCustomerId(Long transactionId, Long bankCustomerId);

	Optional<Transaction> findByReferenceNoAndBankCustomer_BankCustomerId(String referenceNo, Long bankCustomerId);

	List<Transaction> findAllByBankCustomer_BankCustomerIdOrderByTransactionDateDesc(Long bankCustomerId);

	boolean existsByReferenceNo(String referenceNo);
}
