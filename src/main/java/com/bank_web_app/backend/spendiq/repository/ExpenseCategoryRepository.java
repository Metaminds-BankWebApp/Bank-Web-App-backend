package com.bank_web_app.backend.spendiq.repository;

import com.bank_web_app.backend.spendiq.entity.ExpenseCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

	List<ExpenseCategory> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

	Optional<ExpenseCategory> findByCategoryIdAndUser_UserId(Long categoryId, Long userId);

	Optional<ExpenseCategory> findByUser_UserIdAndCategoryNameIgnoreCase(Long userId, String categoryName);

	boolean existsByUser_UserIdAndCategoryNameIgnoreCase(Long userId, String categoryName);
}