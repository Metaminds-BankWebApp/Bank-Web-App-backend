package com.bank_web_app.backend.creditlens.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerIncomeRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLiabilityRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLoanRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerMissedPaymentRepository;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerCardRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerIncomeRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLiabilityRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLoanRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerMissedPaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditEvaluationScoringServiceTest {

	private static final Long PUBLIC_RECORD_ID = 101L;
	private static final Long BANK_RECORD_ID = 201L;

	@Mock
	private PublicCustomerIncomeRepository publicIncomeRepository;

	@Mock
	private PublicCustomerLoanRepository publicLoanRepository;

	@Mock
	private PublicCustomerCardRepository publicCardRepository;

	@Mock
	private PublicCustomerLiabilityRepository publicLiabilityRepository;

	@Mock
	private PublicCustomerMissedPaymentRepository publicMissedPaymentRepository;

	@Mock
	private BankCustomerIncomeRepository bankIncomeRepository;

	@Mock
	private BankCustomerLoanRepository bankLoanRepository;

	@Mock
	private BankCustomerCardRepository bankCardRepository;

	@Mock
	private BankCustomerLiabilityRepository bankLiabilityRepository;

	@Mock
	private BankCustomerMissedPaymentRepository bankMissedPaymentRepository;

	private CreditEvaluationScoringService scoringService;

	@BeforeEach
	void setUp() {
		scoringService = new CreditEvaluationScoringService(
			publicIncomeRepository,
			publicLoanRepository,
			publicCardRepository,
			publicLiabilityRepository,
			publicMissedPaymentRepository,
			bankIncomeRepository,
			bankLoanRepository,
			bankCardRepository,
			bankLiabilityRepository,
			bankMissedPaymentRepository
		);
	}

	@ParameterizedTest
	@MethodSource("salaryRiskCases")
	void calculatesSalaryRiskUsingSalaryTypeEmploymentAndTenure(
		String salaryType,
		String employmentType,
		Integer durationMonths,
		int expectedPoints
	) {
		PublicCustomerIncome salary = publicSalary(salaryType, employmentType, durationMonths);

		assertThat(publicMetrics(List.of(salary)).incomeStabilityPoints()).isEqualTo(expectedPoints);
	}

	private static Stream<Arguments> salaryRiskCases() {
		return Stream.of(
			Arguments.of("FIXED", "PERMANENT", null, 0),
			Arguments.of("FIXED_BASIC_SALARY", "PERMANENT", 18, 0),
			Arguments.of("FIXED", "PERMANENT", 9, 0),
			Arguments.of("FIXED", "PERMANENT", 4, 0),
			Arguments.of("FIXED", "CONTRACT", null, 15),
			Arguments.of("FIXED", "CONTRACT", 9, 8),
			Arguments.of("FIXED", "CONTRACT", 4, 15),
			Arguments.of("AVERAGE_VARIABLE", "PERMANENT", null, 8),
			Arguments.of("Average (Variable)", "CONTRACT", 12, 8),
			Arguments.of("Average (Variable)", "CONTRACT", 4, 15),
			Arguments.of("FIXED", "PERMANANT", null, 0)
		);
	}

	@ParameterizedTest
	@MethodSource("businessRiskCases")
	void retainsBusinessStabilityRules(String stability, int expectedPoints) {
		assertThat(publicMetrics(List.of(publicBusiness(stability))).incomeStabilityPoints()).isEqualTo(expectedPoints);
	}

	private static Stream<Arguments> businessRiskCases() {
		return Stream.of(
			Arguments.of("STABLE", 0),
			Arguments.of("MEDIUM_FLUCTUATION", 8),
			Arguments.of("HIGH_FLUCTUATION", 15)
		);
	}

	@Test
	void dividesTheFifteenPointBudgetAcrossAllIncomeRows() {
		List<PublicCustomerIncome> incomes = List.of(
			publicSalary("FIXED", "PERMANENT", null),
			publicSalary("FIXED", "CONTRACT", 12)
		);

		assertThat(publicMetrics(incomes).incomeStabilityPoints()).isEqualTo(4);
	}

	@Test
	void appliesTheSameSalaryRulesToBankCustomers() {
		BankCustomerFinancialRecord record = new BankCustomerFinancialRecord();
		record.setBankRecordId(BANK_RECORD_ID);
		BankCustomerIncome salary = new BankCustomerIncome();
		salary.setFinancialRecord(record);
		salary.setIncomeCategory("SALARY");
		salary.setAmount(new BigDecimal("150000.00"));
		salary.setSalaryType("FIXED");
		salary.setEmploymentType("PERMANENT");

		when(bankIncomeRepository.findAllByFinancialRecord_BankRecordId(BANK_RECORD_ID)).thenReturn(List.of(salary));
		when(bankLoanRepository.findAllByFinancialRecord_BankRecordId(BANK_RECORD_ID)).thenReturn(List.of());
		when(bankCardRepository.findAllByFinancialRecord_BankRecordId(BANK_RECORD_ID)).thenReturn(List.of());
		when(bankLiabilityRepository.findAllByFinancialRecord_BankRecordId(BANK_RECORD_ID)).thenReturn(List.of());
		when(bankMissedPaymentRepository.findByFinancialRecord_BankRecordId(BANK_RECORD_ID)).thenReturn(Optional.empty());

		assertThat(scoringService.buildBankEvaluationMetrics(record).incomeStabilityPoints()).isZero();
	}

	private EvaluationMetrics publicMetrics(List<PublicCustomerIncome> incomes) {
		PublicCustomerFinancialRecord record = new PublicCustomerFinancialRecord();
		record.setRecordId(PUBLIC_RECORD_ID);
		when(publicIncomeRepository.findAllByFinancialRecord_RecordId(PUBLIC_RECORD_ID)).thenReturn(incomes);
		when(publicLoanRepository.findAllByFinancialRecord_RecordId(PUBLIC_RECORD_ID)).thenReturn(List.of());
		when(publicCardRepository.findAllByFinancialRecord_RecordId(PUBLIC_RECORD_ID)).thenReturn(List.of());
		when(publicLiabilityRepository.findAllByFinancialRecord_RecordId(PUBLIC_RECORD_ID)).thenReturn(List.of());
		when(publicMissedPaymentRepository.findByFinancialRecord_RecordId(PUBLIC_RECORD_ID)).thenReturn(Optional.empty());
		return scoringService.buildPublicEvaluationMetrics(record);
	}

	private PublicCustomerIncome publicSalary(String salaryType, String employmentType, Integer durationMonths) {
		PublicCustomerIncome income = basePublicIncome("SALARY");
		income.setSalaryType(salaryType);
		income.setEmploymentType(employmentType);
		income.setDurationMonths(durationMonths);
		return income;
	}

	private PublicCustomerIncome publicBusiness(String stability) {
		PublicCustomerIncome income = basePublicIncome("BUSINESS");
		income.setIncomeStability(stability);
		return income;
	}

	private PublicCustomerIncome basePublicIncome(String category) {
		PublicCustomerFinancialRecord record = new PublicCustomerFinancialRecord();
		record.setRecordId(PUBLIC_RECORD_ID);
		PublicCustomerIncome income = new PublicCustomerIncome();
		income.setFinancialRecord(record);
		income.setIncomeCategory(category);
		income.setAmount(new BigDecimal("100000.00"));
		return income;
	}
}
