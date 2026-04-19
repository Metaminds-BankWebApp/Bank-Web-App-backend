package com.bank_web_app.backend.bankofficer.repository;

import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankOfficerRepository extends JpaRepository<BankOfficer, Long> {

	Optional<BankOfficer> findByUser_UserId(Long userId);

	boolean existsByEmployeeCode(String employeeCode);
}
