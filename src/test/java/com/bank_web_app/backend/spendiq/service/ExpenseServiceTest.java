package com.bank_web_app.backend.spendiq.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseCategoryRequest;
import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseRecordRequest;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseCategoryResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseRecordResponse;
import com.bank_web_app.backend.spendiq.entity.Expense;
import com.bank_web_app.backend.spendiq.entity.ExpenseCategory;
import com.bank_web_app.backend.spendiq.entity.PaymentMethod;
import com.bank_web_app.backend.spendiq.repository.BudgetLimitRepository;
import com.bank_web_app.backend.spendiq.repository.ExpenseCategoryRepository;
import com.bank_web_app.backend.spendiq.repository.ExpenseRepository;
import com.bank_web_app.backend.spendiq.repository.IncomeRecordRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

	@Mock private ExpenseCategoryRepository categoryRepository;
	@Mock private ExpenseRepository expenseRepository;
	@Mock private IncomeRecordRepository incomeRecordRepository;
	@Mock private BudgetLimitRepository budgetLimitRepository;
	@Mock private UserRepository userRepository;
	@Mock private SpendIqReportPdfExportService pdfExportService;
	@Mock private NotificationEventPublisher notificationEventPublisher;

	private ExpenseService expenseService;
	private User user;

	@BeforeEach
	void setUp() {
		expenseService = new ExpenseService(
			categoryRepository,
			expenseRepository,
			incomeRecordRepository,
			budgetLimitRepository,
			userRepository,
			pdfExportService,
			notificationEventPublisher
		);
		user = new User();
		user.setUserId(12L);
		user.setUsername("alice");
		user.setEmail("alice@example.com");
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("alice", null, List.of())
		);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createsNormalizedExpenseCategoryForLoggedInUser() {
		stubLoggedInUser();
		when(categoryRepository.existsByUser_UserIdAndCategoryNameIgnoreCase(12L, "Food")).thenReturn(false);
		when(categoryRepository.save(any(ExpenseCategory.class))).thenAnswer(invocation -> {
			ExpenseCategory category = invocation.getArgument(0);
			category.setCategoryId(5L);
			return category;
		});

		ExpenseCategoryResponse response = expenseService.createCategory(
			new CreateExpenseCategoryRequest(" Food ", "variable")
		);

		assertThat(response.categoryId()).isEqualTo(5L);
		assertThat(response.userId()).isEqualTo(12L);
		assertThat(response.categoryName()).isEqualTo("Food");
		assertThat(response.categoryType()).isEqualTo("VARIABLE");
	}

	@Test
	void rejectsDuplicateCategoryForSameUser() {
		stubLoggedInUser();
		when(categoryRepository.existsByUser_UserIdAndCategoryNameIgnoreCase(12L, "Food")).thenReturn(true);

		assertThatThrownBy(() -> expenseService.createCategory(new CreateExpenseCategoryRequest("Food", "FIXED")))
			.isInstanceOfSatisfying(ResponseStatusException.class, exception ->
				assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
			);
		verify(categoryRepository, never()).save(any());
	}

	@Test
	void createsExpenseOnlyInsideOwnedCategory() {
		stubLoggedInUser();
		ExpenseCategory category = category(5L, "Food");
		when(categoryRepository.findByCategoryIdAndUser_UserId(5L, 12L)).thenReturn(Optional.of(category));
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
			Expense expense = invocation.getArgument(0);
			expense.setExpenseId(20L);
			return expense;
		});
		when(budgetLimitRepository.findByUser_UserIdAndCategory_CategoryIdAndMonthAndYear(12L, 5L, 8, 2026))
			.thenReturn(Optional.empty());

		ExpenseRecordResponse response = expenseService.createExpense(
			new CreateExpenseRecordRequest(
				5L,
				new BigDecimal("1250.00"),
				LocalDate.of(2026, 8, 20),
				PaymentMethod.CARD,
				null,
				null,
				null,
				null
			)
		);

		assertThat(response.expenseId()).isEqualTo(20L);
		assertThat(response.userId()).isEqualTo(12L);
		assertThat(response.categoryName()).isEqualTo("Food");
		assertThat(response.amount()).isEqualByComparingTo("1250.00");
		assertThat(response.paymentType()).isEqualTo("CARD");
	}

	@Test
	void doesNotImportSameTransactionIntoSpendIqTwice() {
		BankCustomer customer = new BankCustomer();
		customer.setUser(user);
		when(expenseRepository.existsByTrackingSourceAndTrackingReference("TRANSACT", "TXN-100"))
			.thenReturn(true);

		expenseService.trackTransactExpenseForBankCustomer(
			customer,
			"TXN-100",
			new BigDecimal("500.00"),
			null,
			"Food"
		);

		verify(expenseRepository, never()).save(any());
		verify(notificationEventPublisher, never()).publish(any(), any(), any(), any(), any());
	}

	private ExpenseCategory category(Long id, String name) {
		ExpenseCategory category = new ExpenseCategory();
		category.setCategoryId(id);
		category.setUser(user);
		category.setCategoryName(name);
		category.setCategoryType("VARIABLE");
		return category;
	}

	private void stubLoggedInUser() {
		when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
	}
}
