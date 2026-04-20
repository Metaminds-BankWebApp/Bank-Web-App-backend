package com.bank_web_app.backend.transact.repository;

import com.bank_web_app.backend.transact.entity.OtpRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpRecordRepository extends JpaRepository<OtpRecord, Long> {

	Optional<OtpRecord> findTopByTransaction_TransactionIdOrderByCreatedAtDesc(Long transactionId);

	List<OtpRecord> findAllByTransaction_TransactionIdOrderByCreatedAtDesc(Long transactionId);
}
