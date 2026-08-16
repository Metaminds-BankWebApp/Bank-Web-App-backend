package com.bank_web_app.backend.bankofficer.repository;

import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankOfficerRepository extends JpaRepository<BankOfficer, Long> {

	Optional<BankOfficer> findByUser_UserId(Long userId);

	List<BankOfficer> findAllByOrderByUpdatedAtDesc();

	List<BankOfficer> findAllByOrderByCreatedAtDesc();

	List<BankOfficer> findAllByBranch_BranchId(Long branchId);

	boolean existsByEmployeeCode(String employeeCode);

	boolean existsByBranch_BranchId(Long branchId);

	long countByBranch_BranchId(Long branchId);
}
