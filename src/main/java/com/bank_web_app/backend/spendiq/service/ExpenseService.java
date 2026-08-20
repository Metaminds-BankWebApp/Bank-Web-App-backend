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
import com.bank_web_app.backend.spendiq.entity.BudgetLimitSource;
import com.bank_web_app.backend.spendiq.entity.Expense;
import com.bank_web_app.backend.spendiq.entity.ExpenseCategory;
import com.bank_web_app.backend.spendiq.entity.IncomeRecord;
import com.bank_web_app.backend.spendiq.entity.PaymentMethod;
import com.bank_web_app.backend.spendiq.repository.BudgetLimitRepository;
import com.bank_web_app.backend.spendiq.repository.ExpenseCategoryRepository;
import com.bank_web_app.backend.spendiq.repository.ExpenseRepository;
import com.bank_web_app.backend.spendiq.repository.IncomeRecordRepository;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.BudgetRow;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.CategoryRow;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.ExpenseRow;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.IncomeRow;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.PredictionRow;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.ScoreReasonRow;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.SpendIqReportPdfModel;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.SuggestionRow;
import com.bank_web_app.backend.spendiq.service.SpendIqReportPdfExportService.SummaryRow;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.notification.event.NotificationEventType;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
	private final SpendIqReportPdfExportService spendIqReportPdfExportService;
	private final NotificationEventPublisher notificationEventPublisher;

	public ExpenseService(
		ExpenseCategoryRepository expenseCategoryRepository,
		ExpenseRepository expenseRepository,
		IncomeRecordRepository incomeRecordRepository,
		BudgetLimitRepository budgetLimitRepository,
		UserRepository userRepository,
		SpendIqReportPdfExportService spendIqReportPdfExportService,
		NotificationEventPublisher notificationEventPublisher
	) {
		this.expenseCategoryRepository = expenseCategoryRepository;
		this.expenseRepository = expenseRepository;
		this.incomeRecordRepository = incomeRecordRepository;
		this.budgetLimitRepository = budgetLimitRepository;
		this.userRepository = userRepository;
		this.spendIqReportPdfExportService = spendIqReportPdfExportService;
		this.notificationEventPublisher = notificationEventPublisher;
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
		Expense savedExpense = expenseRepository.save(expense);
		notificationEventPublisher.publish(
			NotificationEventType.SPENDIQ_TRANSFER_IMPORTED,
			bankCustomer.getUser().getUserId(),
			null,
			savedExpense.getExpenseId(),
			Map.of(
				"expenseId", String.valueOf(savedExpense.getExpenseId()),
				"referenceNo", normalizedReferenceNo
			)
		);
		publishBudgetThresholdIfReached(bankCustomer.getUser(), savedExpense);
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
		Expense savedExpense = expenseRepository.save(expense);
		publishBudgetThresholdIfReached(user, savedExpense);
		return toExpenseResponse(savedExpense);
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
		Expense savedExpense = expenseRepository.save(expense);
		publishBudgetThresholdIfReached(user, savedExpense);
		return toExpenseResponse(savedExpense);
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
		budgetLimit.setSource(BudgetLimitSource.MANUAL);

		return toBudgetResponse(budgetLimitRepository.save(budgetLimit));
	}

	@Transactional
	public List<BudgetLimitResponse> copyPreviousMonthBudgets(Integer month, Integer year) {
		User user = resolveLoggedInUser();
		YearMonth target = YearMonth.of(year, month);
		YearMonth previous = target.minusMonths(1);

		List<BudgetLimit> previousBudgets = budgetLimitRepository.findAllByUser_UserIdAndMonthAndYearOrderByCreatedAtDesc(
			user.getUserId(),
			previous.getMonthValue(),
			previous.getYear()
		);
		if (previousBudgets.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No budgets found for the previous month to copy.");
		}

		List<BudgetLimit> copied = new ArrayList<>();
		for (BudgetLimit previousBudget : previousBudgets) {
			boolean alreadyExists = budgetLimitRepository
				.findByUser_UserIdAndCategory_CategoryIdAndMonthAndYear(
					user.getUserId(),
					previousBudget.getCategory().getCategoryId(),
					target.getMonthValue(),
					target.getYear()
				)
				.isPresent();
			if (alreadyExists) continue;

			BudgetLimit newBudget = new BudgetLimit();
			newBudget.setUser(user);
			newBudget.setCategory(previousBudget.getCategory());
			newBudget.setBudgetAmount(previousBudget.getBudgetAmount());
			newBudget.setMonth(target.getMonthValue());
			newBudget.setYear(target.getYear());
			newBudget.setSource(BudgetLimitSource.ROLLOVER);
			copied.add(budgetLimitRepository.save(newBudget));
		}

		if (copied.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "All categories already have a budget for this month.");
		}

		return copied.stream().map(this::toBudgetResponse).toList();
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

	@Transactional(readOnly = true)
	public byte[] downloadSpendIqReportPdf(Integer month, Integer year) {
		validateMonthYearFilter(month, year);
		User user = resolveLoggedInUser();
		Long userId = user.getUserId();
		boolean allPeriods = month == null && year == null;

		List<Expense> expenses;
		List<IncomeRecord> incomes;
		List<BudgetLimit> budgets;
		String periodLabel;
		if (allPeriods) {
			expenses = expenseRepository.findAllByUser_UserIdOrderByExpenseDateDescCreatedAtDesc(userId);
			incomes = incomeRecordRepository.findAllByUser_UserIdOrderByIncomeDateDescCreatedAtDesc(userId);
			budgets = budgetLimitRepository.findAllByUser_UserIdOrderByYearDescMonthDescCreatedAtDesc(userId);
			periodLabel = "All periods";
		} else {
			LocalDate startDate = LocalDate.of(year, month, 1);
			LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
			expenses = expenseRepository.findAllByUser_UserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(userId, startDate, endDate);
			incomes = incomeRecordRepository.findAllByUser_UserIdAndIncomeDateBetweenOrderByIncomeDateDescCreatedAtDesc(userId, startDate, endDate);
			budgets = budgetLimitRepository.findAllByUser_UserIdAndMonthAndYearOrderByCreatedAtDesc(userId, month, year);
			periodLabel = startDate.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH));
		}

		List<ExpenseCategory> categories = expenseCategoryRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);
		SummaryRow summary = buildReportSummary(expenses, incomes, budgets);
		List<CategoryRow> categoryRows = buildCategoryRows(categories, expenses);
		BigDecimal fixedExpenses = categoryRows
			.stream()
			.filter(row -> "FIXED".equalsIgnoreCase(row.type()))
			.map(CategoryRow::amount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal variableExpenses = categoryRows
			.stream()
			.filter(row -> !"FIXED".equalsIgnoreCase(row.type()))
			.map(CategoryRow::amount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		ScoreContext scoreContext = buildScoreContext(summary, categoryRows, expenses);
		List<ScoreReasonRow> scoreReasons = buildScoreReasons(scoreContext, summary);
		List<SuggestionRow> suggestions = buildReportSuggestions(scoreContext, summary);
		PredictionRow prediction = buildPrediction(summary, allPeriods ? expenses : expenseRepository.findAllByUser_UserIdOrderByExpenseDateDescCreatedAtDesc(userId), allPeriods ? incomes : incomeRecordRepository.findAllByUser_UserIdOrderByIncomeDateDescCreatedAtDesc(userId));

		return spendIqReportPdfExportService.exportReport(
			new SpendIqReportPdfModel(
				buildCustomerName(user),
				periodLabel,
				LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH)),
				summary,
				scoreContext.score(),
				scoreContext.scoreLabel(),
				fixedExpenses,
				variableExpenses,
				scoreContext.highValueCount(),
				scoreReasons,
				suggestions,
				prediction,
				categoryRows,
				budgets.stream().map(this::toBudgetReportRow).toList(),
				incomes.stream().map(this::toIncomeReportRow).toList(),
				expenses.stream().map(this::toExpenseReportRow).toList()
			)
		);
	}

	private void validateMonthYearFilter(Integer month, Integer year) {
		if ((month == null) != (year == null)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month and year must be provided together.");
		}
		if (month != null && (month < 1 || month > 12)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be between 1 and 12.");
		}
		if (year != null && (year < 2000 || year > 2100)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year must be between 2000 and 2100.");
		}
	}

	private SummaryRow buildReportSummary(List<Expense> expenses, List<IncomeRecord> incomes, List<BudgetLimit> budgets) {
		BigDecimal totalExpense = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalIncome = incomes.stream().map(IncomeRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalBudget = budgets.stream().map(BudgetLimit::getBudgetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal netSavings = totalIncome.subtract(totalExpense);
		BigDecimal remainingBudget = totalBudget.subtract(totalExpense);
		BigDecimal budgetUsage = BigDecimal.ZERO;
		if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
			budgetUsage = totalExpense.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
		}
		return new SummaryRow(totalIncome, totalExpense, totalBudget, netSavings, remainingBudget, budgetUsage);
	}

	private List<CategoryRow> buildCategoryRows(List<ExpenseCategory> categories, List<Expense> expenses) {
		Map<Long, CategoryAccumulator> rows = new LinkedHashMap<>();
		for (ExpenseCategory category : categories) {
			rows.put(
				category.getCategoryId(),
				new CategoryAccumulator(category.getCategoryName(), category.getCategoryType(), BigDecimal.ZERO, 0)
			);
		}
		for (Expense expense : expenses) {
			ExpenseCategory category = expense.getCategory();
			CategoryAccumulator row = rows.getOrDefault(
				category.getCategoryId(),
				new CategoryAccumulator(category.getCategoryName(), category.getCategoryType(), BigDecimal.ZERO, 0)
			);
			rows.put(category.getCategoryId(), row.add(expense.getAmount()));
		}
		return rows
			.values()
			.stream()
			.map(row -> new CategoryRow(row.name(), row.type(), row.count(), row.amount()))
			.sorted(Comparator.comparing(CategoryRow::amount).reversed().thenComparing(CategoryRow::category))
			.toList();
	}

	private ScoreContext buildScoreContext(SummaryRow summary, List<CategoryRow> categoryRows, List<Expense> expenses) {
		BigDecimal totalExpense = summary.totalExpense();
		BigDecimal totalIncome = summary.totalIncome();
		BigDecimal totalBudget = summary.totalBudget();
		double budgetUsage = summary.budgetUsagePercentage().doubleValue();
		double savingsRatio = totalIncome.compareTo(BigDecimal.ZERO) > 0
			? summary.netSavings().divide(totalIncome, 4, RoundingMode.HALF_UP).doubleValue()
			: -0.25d;
		CategoryRow topCategory = categoryRows.stream().filter(row -> row.amount().compareTo(BigDecimal.ZERO) > 0).findFirst().orElse(null);
		double topCategoryShare = topCategory != null && totalExpense.compareTo(BigDecimal.ZERO) > 0
			? topCategory.amount().divide(totalExpense, 4, RoundingMode.HALF_UP).doubleValue()
			: 0d;
		BigDecimal averageExpense = expenses.isEmpty()
			? BigDecimal.ZERO
			: totalExpense.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
		BigDecimal highValueThreshold = averageExpense.multiply(new BigDecimal("1.5")).max(new BigDecimal("50000"));
		int highValueCount = (int) expenses.stream().filter(expense -> expense.getAmount().compareTo(highValueThreshold) > 0).count();

		double budgetPenalty = totalBudget.compareTo(BigDecimal.ZERO) <= 0
			? 22d
			: budgetUsage > 100d
				? Math.min(32d, ((budgetUsage - 100d) * 0.45d) + 18d)
				: Math.max(0d, (budgetUsage - 70d) * 0.25d);
		double savingsPenalty = savingsRatio < 0d
			? Math.min(34d, (Math.abs(savingsRatio) * 70d) + 16d)
			: savingsRatio < 0.1d ? 10d : 0d;
		double concentrationPenalty = topCategoryShare > 0.5d ? Math.min(16d, ((topCategoryShare - 0.5d) * 45d) + 6d) : 0d;
		double highValuePenalty = Math.min(12d, highValueCount * 3d);
		int score = (int) Math.max(0d, Math.min(100d, Math.round(82d - budgetPenalty - savingsPenalty - concentrationPenalty - highValuePenalty)));
		String scoreLabel = score >= 75 ? "Strong" : score >= 50 ? "Moderate" : "Needs attention";

		return new ScoreContext(score, scoreLabel, budgetUsage, budgetPenalty, savingsPenalty, concentrationPenalty, highValuePenalty, highValueCount, topCategory, topCategoryShare);
	}

	private List<ScoreReasonRow> buildScoreReasons(ScoreContext scoreContext, SummaryRow summary) {
		List<ScoreReasonRow> reasons = new ArrayList<>();
		if (scoreContext.budgetPenalty() > 0d) {
			String detail = summary.totalBudget().compareTo(BigDecimal.ZERO) <= 0
				? "No budget is set for this report period, so SpendIQ cannot confirm that spending is controlled."
				: "Budget usage is " + formatPercentage(scoreContext.budgetUsage()) + ", so the score drops when spending is close to or over the budget limit.";
			reasons.add(new ScoreReasonRow("Budget health", Math.round((float) scoreContext.budgetPenalty()), detail));
		}
		if (scoreContext.savingsPenalty() > 0d) {
			String detail = summary.netSavings().compareTo(BigDecimal.ZERO) < 0
				? "Net savings are negative (" + formatCurrency(summary.netSavings()) + "), meaning recorded expenses are higher than recorded income."
				: "Savings are below 10% of recorded income, so SpendIQ marks this as weak savings behavior.";
			reasons.add(new ScoreReasonRow("Savings health", Math.round((float) scoreContext.savingsPenalty()), detail));
		}
		if (scoreContext.concentrationPenalty() > 0d && scoreContext.topCategory() != null) {
			reasons.add(new ScoreReasonRow(
				"Category concentration",
				Math.round((float) scoreContext.concentrationPenalty()),
				scoreContext.topCategory().category() + " is " + formatPercentage(scoreContext.topCategoryShare() * 100d) + " of total spend, so spending is too concentrated in one category."
			));
		}
		if (scoreContext.highValuePenalty() > 0d) {
			reasons.add(new ScoreReasonRow(
				"High-value transactions",
				Math.round((float) scoreContext.highValuePenalty()),
				scoreContext.highValueCount() + " transaction" + (scoreContext.highValueCount() == 1 ? " is" : "s are") + " unusually high compared with the average expense."
			));
		}
		if (reasons.isEmpty()) {
			reasons.add(new ScoreReasonRow("Healthy behavior", 0, "Budget usage, savings, category spread, and transaction values are not creating major score penalties."));
		}
		return reasons;
	}

	private List<SuggestionRow> buildReportSuggestions(ScoreContext scoreContext, SummaryRow summary) {
		List<SuggestionRow> suggestions = new ArrayList<>();
		if (scoreContext.budgetUsage() > 100d) {
			suggestions.add(new SuggestionRow("Reduce overspending against budgets", "Budget usage is " + formatPercentage(scoreContext.budgetUsage()) + ", so review categories that crossed their limits before adding new discretionary spend."));
		} else if (scoreContext.budgetUsage() >= 80d) {
			suggestions.add(new SuggestionRow("Slow spending before month end", "Budget usage is already " + formatPercentage(scoreContext.budgetUsage()) + ", so keep variable expenses controlled for the rest of the period."));
		} else if (summary.totalBudget().compareTo(BigDecimal.ZERO) <= 0) {
			suggestions.add(new SuggestionRow("Create monthly category budgets", "No active budget is available for this report period, so add limits for your main categories to unlock stronger SpendIQ guidance."));
		}
		if (scoreContext.topCategory() != null) {
			suggestions.add(new SuggestionRow("Watch " + scoreContext.topCategory().category(), scoreContext.topCategory().category() + " is the largest spending category at " + formatCurrency(scoreContext.topCategory().amount()) + "."));
		}
		if (summary.netSavings().compareTo(BigDecimal.ZERO) < 0) {
			suggestions.add(new SuggestionRow("Protect monthly savings", "Expenses are higher than income for this period. Try setting a savings-first target before planning variable purchases."));
		} else {
			suggestions.add(new SuggestionRow("Keep savings momentum", "Net savings are positive at " + formatCurrency(summary.netSavings()) + ". Consider moving part of this surplus into a savings goal."));
		}
		if (scoreContext.highValueCount() > 0) {
			suggestions.add(new SuggestionRow("Review high value transactions", scoreContext.highValueCount() + " transaction" + (scoreContext.highValueCount() == 1 ? "" : "s") + " stand out as high value. Confirm they are planned and not repeated unnecessarily."));
		}
		return suggestions.stream().limit(4).toList();
	}

	private PredictionRow buildPrediction(SummaryRow summary, List<Expense> allExpenses, List<IncomeRecord> allIncomes) {
		BigDecimal recentSpend = averageLastThreeMonthsExpense(allExpenses);
		BigDecimal predictedSpend = recentSpend.multiply(new BigDecimal("0.70"))
			.add(summary.totalExpense().multiply(new BigDecimal("0.30")))
			.setScale(2, RoundingMode.HALF_UP);
		BigDecimal averageIncome = averageLastThreeMonthsIncome(allIncomes);
		BigDecimal predictedSavings = averageIncome.subtract(predictedSpend).setScale(2, RoundingMode.HALF_UP);
		boolean positive = predictedSavings.compareTo(BigDecimal.ZERO) >= 0;
		return new PredictionRow(
			positive ? "Positive savings likely" : "Savings risk likely",
			predictedSpend.max(BigDecimal.ZERO),
			predictedSavings,
			positive
				? "Based on recent records, next month is likely to stay within a manageable spending range if category behavior remains similar."
				: "Recent spending is trending above income, so next month may produce negative savings unless high-spend categories are reduced."
		);
	}

	private BigDecimal averageLastThreeMonthsExpense(List<Expense> expenses) {
		Map<String, BigDecimal> grouped = new LinkedHashMap<>();
		for (Expense expense : expenses) {
			String key = expense.getExpenseDate().getYear() + "-" + expense.getExpenseDate().getMonthValue();
			grouped.put(key, grouped.getOrDefault(key, BigDecimal.ZERO).add(expense.getAmount()));
		}
		return averageRecentGroupedValues(grouped);
	}

	private BigDecimal averageLastThreeMonthsIncome(List<IncomeRecord> incomes) {
		Map<String, BigDecimal> grouped = new LinkedHashMap<>();
		for (IncomeRecord income : incomes) {
			String key = income.getIncomeDate().getYear() + "-" + income.getIncomeDate().getMonthValue();
			grouped.put(key, grouped.getOrDefault(key, BigDecimal.ZERO).add(income.getAmount()));
		}
		return averageRecentGroupedValues(grouped);
	}

	private BigDecimal averageRecentGroupedValues(Map<String, BigDecimal> grouped) {
		List<BigDecimal> values = grouped.values().stream().limit(3).toList();
		if (values.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
	}

	private BudgetRow toBudgetReportRow(BudgetLimit budget) {
		return new BudgetRow(budget.getCategory().getCategoryName(), budget.getMonth(), budget.getYear(), budget.getBudgetAmount());
	}

	private IncomeRow toIncomeReportRow(IncomeRecord income) {
		return new IncomeRow(income.getIncomeDate().toString(), income.getSourceName(), income.getAmount());
	}

	private ExpenseRow toExpenseReportRow(Expense expense) {
		return new ExpenseRow(expense.getExpenseDate().toString(), expense.getCategory().getCategoryName(), expense.getPaymentType().name(), expense.getAmount());
	}

	private String buildCustomerName(User user) {
		String firstName = normalizeText(user.getFirstName());
		String lastName = normalizeText(user.getLastName());
		String fullName = (firstName + " " + lastName).trim();
		return fullName.isBlank() ? user.getUsername() : fullName;
	}

	private String formatCurrency(BigDecimal value) {
		return "LKR " + (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private String formatPercentage(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
	}

	private record CategoryAccumulator(String name, String type, BigDecimal amount, int count) {
		private CategoryAccumulator add(BigDecimal nextAmount) {
			return new CategoryAccumulator(name, type, amount.add(nextAmount == null ? BigDecimal.ZERO : nextAmount), count + 1);
		}
	}

	private record ScoreContext(
		int score,
		String scoreLabel,
		double budgetUsage,
		double budgetPenalty,
		double savingsPenalty,
		double concentrationPenalty,
		double highValuePenalty,
		int highValueCount,
		CategoryRow topCategory,
		double topCategoryShare
	) {}

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

	private void publishBudgetThresholdIfReached(User user, Expense expense) {
		int month = expense.getExpenseDate().getMonthValue();
		int year = expense.getExpenseDate().getYear();
		BudgetLimit budget = budgetLimitRepository
			.findByUser_UserIdAndCategory_CategoryIdAndMonthAndYear(
				user.getUserId(),
				expense.getCategory().getCategoryId(),
				month,
				year
			)
			.orElse(null);
		if (budget == null || budget.getBudgetAmount() == null || budget.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		BigDecimal categoryTotal = expenseRepository
			.findAllByUser_UserIdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
				user.getUserId(),
				expense.getExpenseDate().withDayOfMonth(1),
				expense.getExpenseDate().withDayOfMonth(expense.getExpenseDate().lengthOfMonth())
			)
			.stream()
			.filter(item -> item.getCategory().getCategoryId().equals(expense.getCategory().getCategoryId()))
			.map(Expense::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal usage = categoryTotal
			.divide(budget.getBudgetAmount(), 4, RoundingMode.HALF_UP)
			.multiply(new BigDecimal("100"));
		String threshold = usage.compareTo(new BigDecimal("100")) >= 0
			? "100"
			: usage.compareTo(new BigDecimal("80")) >= 0 ? "80" : null;
		if (threshold == null) return;

		notificationEventPublisher.publish(
			NotificationEventType.SPENDIQ_BUDGET_THRESHOLD,
			user.getUserId(),
			user.getUserId(),
			budget.getBudgetId(),
			Map.of(
				"categoryId", String.valueOf(expense.getCategory().getCategoryId()),
				"categoryName", expense.getCategory().getCategoryName(),
				"month", String.valueOf(month),
				"year", String.valueOf(year),
				"threshold", threshold
			)
		);
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
			budgetLimit.getSource() == BudgetLimitSource.ROLLOVER,
			budgetLimit.getCreatedAt(),
			budgetLimit.getUpdatedAt()
		);
	}
}
