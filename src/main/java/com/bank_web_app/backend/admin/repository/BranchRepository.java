package com.bank_web_app.backend.admin.repository;

import com.bank_web_app.backend.admin.entity.Branch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

	Optional<Branch> findByBranchCode(String branchCode);

	boolean existsByBranchCode(String branchCode);
}
