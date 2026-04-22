package com.bank_web_app.backend.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bank_web_app.backend.admin.entity.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {

	Optional<Branch> findByBranchCode(String branchCode);

	boolean existsByBranchCode(String branchCode);

	@Query(value = "select nextval('branch_code_seq')", nativeQuery = true)
	Long getNextBranchCodeSequence();
}
