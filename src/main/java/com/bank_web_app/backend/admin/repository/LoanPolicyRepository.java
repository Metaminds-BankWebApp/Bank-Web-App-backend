package com.bank_web_app.backend.admin.repository;

import com.bank_web_app.backend.admin.entity.LoanPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanPolicyRepository extends JpaRepository<LoanPolicy, Long> {

	List<LoanPolicy> findAllByOrderByLoanTypeAsc();

	List<LoanPolicy> findAllByStatusOrderByLoanTypeAsc(String status);

	Optional<LoanPolicy> findByLoanType(String loanType);
}
