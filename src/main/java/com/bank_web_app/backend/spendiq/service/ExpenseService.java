package com.bank_web_app.backend.spendiq.service;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseCategoryRequest;
import com.bank_web_app.backend.spendiq.dto.request.CreateExpenseRecordRequest;
import com.bank_web_app.backend.spendiq.dto.request.CreateIncomeRecordRequest;
import com.bank_web_app.backend.spendiq.dto.request.UpsertBudgetLimitRequest;
import com.bank_web_app.backend.spendiq.dto.response.BudgetLimitResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseCategoryResponse;
import com.bank_web_app.backend.spendiq.dto.response.ExpenseRecordResponse;
import com.bank_web_app.backend.spendiq.dto.response.IncomeRecordResponse;
import com.bank_web_app.backend.spendiq.dto.response.SpendIqMonthlySummaryResponse;
import com.bank_web_app.backend.spendiq.entity.BudgetLimit;
import com.bank_web_app.backend.spendiq.entity.Expense;
import com.bank_web_app.backend.spendiq.entity.ExpenseCategory;
import com.bank_web_app.backend.spendiq.entity.IncomeRecord;
import com.bank_web_app.backend.spendiq.entity.PaymentMethod;
import com.bank_web_app.backend.spendiq.repository.BudgetLimitRepository;
import com.bank_web_app.backend.spendiq.repository.ExpenseCategoryRepository;
import com.bank_web_app.backend.spendiq.repository.ExpenseRepository;
import com.bank_web_app.backend.spendiq.repository.IncomeRecordRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExpenseService {

	private static final String SOURCE_TRANSACT = "TRANSACT";
	private static final String DEFAULT_TRANSFER_CATEGORY_NAME = "Bank Transfer";
	private static final String DEFAULT_TRANSFER_CATEGORY_TYPE = "VARIABLE";
	private static final PaymentMethod DEFAULT_TRANSFER_PAYMENT_TYPE = PaymentMethod.BANK_TRANSFER;
	private static final List<DefaultCategorySeed> DEFAULT_SPENDIQ_CATEGORIES = List.of(
		new DefaultCategorySeed("Food", "VARIABLE"),
		new DefaultCategorySeed("Transport", "VARIABLE"),
		new DefaultCategorySeed("Bills", "FIXED"),
		new DefaultCategorySeed("Shopping", "VARIABLE"),
		new DefaultCategorySeed("Health", "VARIABLE"),
		new DefaultCategorySeed("Education", "FIXED"),
		new DefaultCategorySeed("Entertainment", "VARIABLE"),
		new DefaultCategorySeed("Savings", "FIXED")
	);

	private final ExpenseCategoryRepository expenseCategoryRepository;
	private final ExpenseRepository expenseRepository;
	private final IncomeRecordRepository incomeRecordRepository;
	private final BudgetLimitRepository budgetLimitRepository;
	private final UserRepository userRepository;

	public ExpenseService(
		ExpenseCategoryRepository expenseCategoryRepository,
		ExpenseRepository expenseRepository,
		IncomeRecordRepository incomeRecordRepository,
		BudgetLimitRepository budgetLimitRepository,
		UserRepository userRepository
	) {
		this.expenseCategoryRepository = expenseCategoryRepository;
		this.expenseRepository = expenseRepository;
		this.incomeRecordRepository = incomeRecordRepository;
		this.budgetLimitRepository = budgetLimitRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public ExpenseCategoryResponse createCategory(CreateExpenseCategoryRequest request) {
		User user = resolveLoggedInUser();
		String categoryName = normalizeText(request.categoryName());
		String categoryType = normalizeCategoryType(request.categoryType());

		if (expenseCategoryRepository.existsByUser_UserIdAndCategoryNameIgnoreCase(user.getUserId(), categoryName)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists for this user.");
		}

		ExpenseCategory category = new ExpenseCategory();
		category.setUser(user);
		category.setCategoryName(categoryName);
		category.setCategoryType(categoryType);
		return toCategoryResponse(expenseCategoryRepository.save(category));
	}

	@Transactional
	public void trackTransactExpenseForBankCustomer(
		BankCustomer bankCustomer,
		String referenceNo,
		BigDecimal amount,
		LocalDateTime transactionDate
	) {
		if (bankCustomer == null || bankCustomer.getUser() == null || bankCustomer.getUser().getUserId() == null) {
			throw new IllegalArgumentException("Bank customer user context is required for SpendIQ tracking.");
		}

		String normalizedReferenceNo = normalizeText(referenceNo);
		if (normalizedReferenceNo.isBlank()) {
			throw new IllegalArgumentException("Transaction reference is required for SpendIQ tracking.");
		}

		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Tracked expense amount must be greater than 0.");
		}

		if (expenseRepository.existsByTrackingSourceAndTrackingReference(SOURCE_TRANSACT, normalizedReferenceNo)) {
			return;
		}

		Long userId = bankCustomer.getUser().getUserId();
		ExpenseCategory category = expenseCategoryRepository
			.findByUser_UserIdAndCategoryNameIgnoreCase(userId, DEFAULT_TRANSFER_CATEGORY_NAME)
			.orElseGet(() -> {
				ExpenseCategory createdCategory = new ExpenseCategory();
				createdCategory.setUser(bankCustomer.getUser());
				createdCategory.setCategoryName(DEFAULT_TRANSFER_CATEGORY_NAME);
				createdCategory.setCategoryType(DEFAULT_TRANSFER_CATEGORY_TYPE);
				return expenseCategoryRepository.save(createdCategory);
			});

		Expense expense = new Expense();
		expense.setUser(bankCustomer.getUser());
		expense.setCategory(category);
		expense.setAmount(amount);
		expense.setExpenseDate(transactionDate == null ? LocalDate.now() : transactionDate.toLocalDate());
		expense.setPaymentType(DEFAULT_TRANSFER_PAYMENT_TYPE);
		expense.setTrackingSource(SOURCE_TRANSACT);
		expense.setTrackingReference(normalizedReferenceNo);
		expenseRepository.save(expense);
	}

	@Transactional
	public List<ExpenseCategoryResponse> getCategories() {
		User user = resolveLoggedInUser();
		ensureDefaultCategories(user);
		return expenseCategoryRepository
			.findAllByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
			.stream()
			.map(this::toCategoryResponse)
			.toList();
	}

	@Transactional
	public ExpenseRecordResponse createExpense(CreateExpenseRecordRequest request) {
		User user = resolveLoggedInUser();
		ExpenseCategory category = resolveExpenseCategoryForCreate(user, request);
		BigDecimal amount = request.amount();
		LocalDate expenseDate = request.expenseDate();
		PaymentMethod paymentType = request.paymentType();

		validatePositiveAmount(amount);
		validateAmountScale(amount);
		if (expenseDate == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expenseDate is required.");
		}
		if (paymentType == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentType is required.");
		}

		Expense expense = new Expense();
		expense.setUser(user);
		expense.setCategory(category);
		expense.setAmount(amount);
		expense.setExpenseDate(expenseDate);
		expense.setPaymentType(paymentType);
		return toExpenseResponse(expenseRepository.save(expense));
	}

	@Transactional
	public ExpenseRecordResponse updateExpense(Long expenseId, CreateExpenseRecordRequest request) {
		User user = resolveLoggedInUser();
		Expense expense = expenseRepository
			.findByExpenseIdAndUser_UserId(expenseId, user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense record not found for this user."));
		ExpenseCategory category = resolveExpenseCategoryForCreate(user, request);

		BigDecimal amount = request.amount();
		LocalDate expenseDate = request.expenseDate();
		PaymentMethod paymentType = request.paymentType();

		validatePositiveAmount(amount);
		validateAmountScale(amount);
		if (expenseDate == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expenseDate is required.");
		}
		if (paymentType == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentType is required.");
		}

		expense.setCategory(category);
		expense.setAmount(amount);
		expense.setExpenseDate(expenseDate);
		expense.setPaymentType(paymentType);
		return toExpenseResponse(expenseRepository.save(expense));
	}

	@Transactional
	public void deleteExpense(Long expenseId) {
		User user = resolveLoggedInUser();
		Expense expense = expenseRepository
			.findByExpenseIdAndUser_UserId(expenseId, user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense record not found for this user."));
		expenseRepository.delete(expense);
	}

	@Transactional(readOnly = true)
	public List<ExpenseRecordResponse> getExpenses(LocalDate fromDate, LocalDate toDate) {
		User user = resolveLoggedInUser();

		List<Expense> expenses;
		if (fromDate != null && toDate != null) {
			if (toDate.isBefore(fromDate)) {
				throw new IllegalArgumentException("toDate must be greater than or equal to fromDate.");
			}
			expenses = expenseRepository.findAllByUser_UserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
				user.getUserId(),
				fromDate,
				toDate
			);
		} else {
			expenses = expenseRepository.findAllByUser_UserIdOrderByExpenseDateDescCreatedAtDesc(user.getUserId());
		}

		return expenses.stream().map(this::toExpenseResponse).toList();
	}

	@Transactional
	public IncomeRecordResponse createIncome(CreateIncomeRecordRequest request) {
		User user = resolveLoggedInUser();
		IncomeRecord incomeRecord = new IncomeRecord();
		incomeRecord.setUser(user);
		incomeRecord.setSourceName(normalizeText(request.sourceName()));
		validatePositiveAmount(request.amount());
		validateAmountScale(request.amount());
		incomeRecord.setAmount(request.amount());
		incomeRecord.setIncomeDate(request.incomeDate());
		return toIncomeResponse(incomeRecordRepository.save(incomeRecord));
	}

	@Transactional
	public IncomeRecordResponse updateIncome(Long incomeId, CreateIncomeRecordRequest request) {
		User user = resolveLoggedInUser();
		IncomeRecord incomeRecord = incomeRecordRepository
			.findByIncomeIdAndUser_UserId(incomeId, user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Income record not found for this user."));

		String sourceName = normalizeText(request.sourceName());
		if (sourceName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceName is required.");
		}
		if (request.incomeDate() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomeDate is required.");
		}
		validatePositiveAmount(request.amount());
		validateAmountScale(request.amount());

		incomeRecord.setSourceName(sourceName);
		incomeRecord.setAmount(request.amount());
		incomeRecord.setIncomeDate(request.incomeDate());
		return toIncomeResponse(incomeRecordRepository.save(incomeRecord));
	}

	@Transactional
	public void deleteIncome(Long incomeId) {
		User user = resolveLoggedInUser();
		IncomeRecord incomeRecord = incomeRecordRepository
			.findByIncomeIdAndUser_UserId(incomeId, user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Income record not found for this user."));
		incomeRecordRepository.delete(incomeRecord);
	}

	@Transactional(readOnly = true)
	public List<IncomeRecordResponse> getIncomes(LocalDate fromDate, LocalDate toDate) {
		User user = resolveLoggedInUser();

		List<IncomeRecord> incomes;
		if (fromDate != null && toDate != null) {
			if (toDate.isBefore(fromDate)) {
				throw new IllegalArgumentException("toDate must be greater than or equal to fromDate.");
			}
			incomes = incomeRecordRepository.findAllByUser_UserIdAndIncomeDateBetweenOrderByIncomeDateDescCreatedAtDesc(
				user.getUserId(),
				fromDate,
				toDate
			);
		} else {
			incomes = incomeRecordRepository.findAllByUser_UserIdOrderByIncomeDateDescCreatedAtDesc(user.getUserId());
		}

		return incomes.stream().map(this::toIncomeResponse).toList();
	}

	@Transactional
	public BudgetLimitResponse upsertBudget(UpsertBudgetLimitRequest request) {
		User user = resolveLoggedInUser();
		ExpenseCategory category = expenseCategoryRepository
			.findByCategoryIdAndUser_UserId(request.categoryId(), user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense category not found for this user."));

		BudgetLimit budgetLimit = budgetLimitRepository
			.findByUser_UserIdAndCategory_CategoryIdAndMonthAndYear(
				user.getUserId(),
				request.categoryId(),
				request.month(),
				request.year()
			)
			.orElseGet(BudgetLimit::new);

		budgetLimit.setUser(user);
		budgetLimit.setCategory(category);
		budgetLimit.setBudgetAmount(request.budgetAmount());
		budgetLimit.setMonth(request.month());
		budgetLimit.setYear(request.year());

		return toBudgetResponse(budgetLimitRepository.save(budgetLimit));
	}

	@Transactional(readOnly = true)
	public List<BudgetLimitResponse> getBudgets(Integer month, Integer year) {
		User user = resolveLoggedInUser();
		List<BudgetLimit> budgets;
		if (month != null && year != null) {
			budgets = budgetLimitRepository.findAllByUser_UserIdAndMonthAndYearOrderByCreatedAtDesc(user.getUserId(), month, year);
		} else {
			budgets = budgetLimitRepository.findAllByUser_UserIdOrderByYearDescMonthDescCreatedAtDesc(user.getUserId());
		}
		return budgets.stream().map(this::toBudgetResponse).toList();
	}

	@Transactional(readOnly = true)
	public SpendIqMonthlySummaryResponse getMonthlySummary(Integer month, Integer year) {
		User user = resolveLoggedInUser();
		LocalDate startDate = LocalDate.of(year, month, 1);
		LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

		BigDecimal totalExpense = expenseRepository
			.findAllByUser_UserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(user.getUserId(), startDate, endDate)
			.stream()
			.map(Expense::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalIncome = incomeRecordRepository
			.findAllByUser_UserIdAndIncomeDateBetweenOrderByIncomeDateDescCreatedAtDesc(user.getUserId(), startDate, endDate)
			.stream()
			.map(IncomeRecord::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalBudget = budgetLimitRepository
			.findAllByUser_UserIdAndMonthAndYearOrderByCreatedAtDesc(user.getUserId(), month, year)
			.stream()
			.map(BudgetLimit::getBudgetAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal netSavings = totalIncome.subtract(totalExpense);
		BigDecimal remainingBudget = totalBudget.subtract(totalExpense);

		BigDecimal budgetUsagePercentage = BigDecimal.ZERO;
		if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
			budgetUsagePercentage = totalExpense
				.divide(totalBudget, 4, RoundingMode.HALF_UP)
				.multiply(new BigDecimal("100"));
		}

		return new SpendIqMonthlySummaryResponse(
			month,
			year,
			totalIncome,
			totalExpense,
			totalBudget,
			netSavings,
			remainingBudget,
			budgetUsagePercentage
		);
	}

	private User resolveLoggedInUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		return userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));
	}

	private String normalizeText(String value) {
		return value == null ? "" : value.trim();
	}

	private String normalizeCategoryType(String value) {
		String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
		if (!"FIXED".equals(normalized) && !"VARIABLE".equals(normalized)) {
			throw new IllegalArgumentException("categoryType must be either FIXED or VARIABLE.");
		}
		return normalized;
	}

	private void validateAmountScale(BigDecimal amount) {
		if (amount == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount is required.");
		}
		if (amount.scale() > 2) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount can have at most 2 decimal places.");
		}
	}

	private void validatePositiveAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be greater than 0.");
		}
	}

	private ExpenseCategory resolveExpenseCategoryForCreate(User user, CreateExpenseRecordRequest request) {
		if (request.categoryId() != null) {
			return expenseCategoryRepository
				.findByCategoryIdAndUser_UserId(request.categoryId(), user.getUserId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense category not found for this user."));
		}

		String legacyCategory = normalizeText(request.category());
		if (legacyCategory.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId or category is required.");
		}

		return expenseCategoryRepository
			.findByUser_UserIdAndCategoryNameIgnoreCase(user.getUserId(), legacyCategory)
			.orElseGet(() -> {
				ExpenseCategory created = new ExpenseCategory();
				created.setUser(user);
				created.setCategoryName(legacyCategory);
				created.setCategoryType("VARIABLE");
				return expenseCategoryRepository.save(created);
			});
	}

	private void ensureDefaultCategories(User user) {
		List<ExpenseCategory> existingCategories = expenseCategoryRepository.findAllByUser_UserIdOrderByCreatedAtDesc(user.getUserId());
		Set<String> existingNames = new LinkedHashSet<>();
		for (ExpenseCategory category : existingCategories) {
			existingNames.add(normalizeText(category.getCategoryName()).toLowerCase(Locale.ROOT));
		}

		List<ExpenseCategory> categoriesToCreate = new ArrayList<>();
		for (DefaultCategorySeed seed : DEFAULT_SPENDIQ_CATEGORIES) {
			String normalizedName = seed.name().toLowerCase(Locale.ROOT);
			if (existingNames.contains(normalizedName)) {
				continue;
			}
			ExpenseCategory category = new ExpenseCategory();
			category.setUser(user);
			category.setCategoryName(seed.name());
			category.setCategoryType(seed.type());
			categoriesToCreate.add(category);
		}

		if (!categoriesToCreate.isEmpty()) {
			expenseCategoryRepository.saveAll(categoriesToCreate);
		}
	}

	private record DefaultCategorySeed(String name, String type) {}

	private ExpenseCategoryResponse toCategoryResponse(ExpenseCategory category) {
		return new ExpenseCategoryResponse(
			category.getCategoryId(),
			category.getUser().getUserId(),
			category.getCategoryName(),
			category.getCategoryType(),
			category.getCreatedAt()
		);
	}

	private ExpenseRecordResponse toExpenseResponse(Expense expense) {
		return new ExpenseRecordResponse(
			expense.getExpenseId(),
			expense.getUser().getUserId(),
			expense.getCategory().getCategoryId(),
			expense.getCategory().getCategoryName(),
			expense.getAmount(),
			expense.getExpenseDate(),
			expense.getPaymentType().name(),
			expense.getCreatedAt()
		);
	}

	private IncomeRecordResponse toIncomeResponse(IncomeRecord incomeRecord) {
		return new IncomeRecordResponse(
			incomeRecord.getIncomeId(),
			incomeRecord.getUser().getUserId(),
			incomeRecord.getSourceName(),
			incomeRecord.getAmount(),
			incomeRecord.getIncomeDate(),
			incomeRecord.getCreatedAt()
		);
	}

	private BudgetLimitResponse toBudgetResponse(BudgetLimit budgetLimit) {
		return new BudgetLimitResponse(
			budgetLimit.getBudgetId(),
			budgetLimit.getUser().getUserId(),
			budgetLimit.getCategory().getCategoryId(),
			budgetLimit.getCategory().getCategoryName(),
			budgetLimit.getBudgetAmount(),
			budgetLimit.getMonth(),
			budgetLimit.getYear(),
			budgetLimit.getCreatedAt(),
			budgetLimit.getUpdatedAt()
		);
	}
}
