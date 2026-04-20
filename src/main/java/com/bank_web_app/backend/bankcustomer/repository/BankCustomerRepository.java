package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerRepository extends JpaRepository<BankCustomer, Long> {

	Optional<BankCustomer> findByUser_UserId(Long userId);

	Optional<BankCustomer> findByUser_UserIdAndOfficer_OfficerId(Long userId, Long officerId);

	List<BankCustomer> findAllByOfficer_OfficerIdOrderByUpdatedAtDesc(Long officerId);

	boolean existsByCustomerCode(String customerCode);

	boolean existsByAccount_AccountId(Long accountId);
}
