package com.bank_web_app.backend.bankcustomer.repository;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCustomerRepository extends JpaRepository<BankCustomer, Long> {

	Optional<BankCustomer> findByUser_UserId(Long userId);

	boolean existsByCustomerCode(String customerCode);
}
