package com.bank_web_app.backend.loansense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.admin.entity.LoanPolicy;
import com.bank_web_app.backend.admin.entity.RiskAdjustment;
import com.bank_web_app.backend.admin.repository.LoanPolicyRepository;
import com.bank_web_app.backend.admin.repository.RiskAdjustmentRepository;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerIncomeRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLiabilityRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLoanRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerMissedPaymentRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.bankofficer.entity.BankOfficer;
import com.bank_web_app.backend.bankofficer.repository.BankOfficerRepository;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.service.CreditEvaluationService;
import com.bank_web_app.backend.loansense.dto.request.CreateLoanSenseEvaluationRequest;
import com.bank_web_app.backend.loansense.dto.request.LoanSenseLoanInputRequest;
import com.bank_web_app.backend.loansense.dto.response.LoanSenseEvaluationResponse;
import com.bank_web_app.backend.loansense.entity.LoanEligibilityResult;
import com.bank_web_app.backend.loansense.entity.LoanSenseEvaluation;
import com.bank_web_app.backend.loansense.mapper.LoanEligibilityMapper;
import com.bank_web_app.backend.loansense.repository.LoanEligibilityRepository;
import com.bank_web_app.backend.notification.event.NotificationEventPublisher;
import com.bank_web_app.backend.notification.event.NotificationEventType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class LoanEligibilityServiceTest {

	@Mock private LoanEligibilityRepository loanEligibilityRepository;
	@Mock private BankCustomerRepository bankCustomerRepository;
	@Mock private BankCustomerFinancialRecordRepository financialRecordRepository;
	@Mock private BankCustomerIncomeRepository incomeRepository;
	@Mock private BankCustomerLoanRepository loanRepository;
	@Mock private BankCustomerCardRepository cardRepository;
	@Mock private BankCustomerLiabilityRepository liabilityRepository;
	@Mock private BankCustomerMissedPaymentRepository missedPaymentRepository;
	@Mock private BankOfficerRepository bankOfficerRepository;
	@Mock private LoanPolicyRepository loanPolicyRepository;
	@Mock private RiskAdjustmentRepository riskAdjustmentRepository;
	@Mock private UserRepository userRepository;
	@Mock private CreditEvaluationService creditEvaluationService;
	@Mock private LoanEligibilityMapper loanEligibilityMapper;
	@Mock private NotificationEventPublisher notificationEventPublisher;
	@Mock private LoanSenseEvaluationResponse mappedResponse;

	private LoanEligibilityService loanEligibilityService;
	private BankCustomer customer;
	private BankCustomerFinancialRecord financialRecord;

	@BeforeEach
	void setUp() {
		loanEligibilityService = new LoanEligibilityService(
			loanEligibilityRepository,
			bankCustomerRepository,
			financialRecordRepository,
			incomeRepository,
			loanRepository,
			cardRepository,
			liabilityRepository,
			missedPaymentRepository,
			bankOfficerRepository,
			loanPolicyRepository,
			riskAdjustmentRepository,
			userRepository,
			creditEvaluationService,
			loanEligibilityMapper,
			notificationEventPublisher
		);
		configureOfficerAndCustomerContext();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createsEligiblePersonalLoanEvaluationFromIncomeAndPolicy() {
		stubFinancialInputs(new BigDecimal("100000.00"), BigDecimal.ZERO);
		stubPolicyAndRisk();
		stubEvaluationSave();

		LoanSenseEvaluationResponse response = loanEligibilityService.createEvaluationForOfficer(
			30L,
			request("PERSONAL")
		);

		assertThat(response).isSameAs(mappedResponse);
		LoanSenseEvaluation evaluation = capturedEvaluation();
		assertThat(evaluation.getMonthlyIncome()).isEqualByComparingTo("100000.00");
		assertThat(evaluation.getMaxAllowedEmi()).isEqualByComparingTo("40000.00");
		assertThat(evaluation.getAvailableEmiCapacity()).isEqualByComparingTo("40000.00");
		assertThat(evaluation.getOverallStatus()).isEqualTo("ELIGIBLE");
		assertThat(evaluation.getResults()).singleElement().satisfies(result -> {
			assertThat(result.getLoanType()).isEqualTo("PERSONAL");
			assertThat(result.getEligibilityStatus()).isEqualTo("ELIGIBLE");
			assertThat(result.getRecommendedMaxAmount()).isPositive();
		});
		verify(notificationEventPublisher).publish(
			any(NotificationEventType.class),
			any(),
			any(),
			any(),
			any()
		);
	}

	@Test
	void marksLoanNotEligibleWhenExistingEmiExceedsAllowedCapacity() {
		stubFinancialInputs(new BigDecimal("100000.00"), new BigDecimal("45000.00"));
		stubPolicyAndRisk();
		stubEvaluationSave();

		loanEligibilityService.createEvaluationForOfficer(30L, request("PERSONAL"));

		LoanSenseEvaluation evaluation = capturedEvaluation();
		assertThat(evaluation.getAvailableEmiCapacity()).isEqualByComparingTo("-5000.00");
		assertThat(evaluation.getOverallStatus()).isEqualTo("NOT_ELIGIBLE");
		assertThat(evaluation.getResults()).singleElement().satisfies(result -> {
			assertThat(result.getEligibilityStatus()).isEqualTo("NOT_ELIGIBLE");
			assertThat(result.getDecisionReason()).contains("allowed EMI capacity");
		});
	}

	@Test
	void rejectsEvaluationWhenMonthlyIncomeIsNotPositive() {
		stubFinancialInputs(BigDecimal.ZERO, BigDecimal.ZERO);

		assertThatThrownBy(() -> loanEligibilityService.createEvaluationForOfficer(30L, request("PERSONAL")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("positive monthly income");
		verify(loanEligibilityRepository, never()).save(any());
	}

	@Test
	void rejectsDuplicateLoanTypesInOfficerRequest() {
		CreateLoanSenseEvaluationRequest duplicateRequest = new CreateLoanSenseEvaluationRequest(
			List.of(
				new LoanSenseLoanInputRequest("PERSONAL", null),
				new LoanSenseLoanInputRequest("personal", null)
			)
		);

		assertThatThrownBy(() -> loanEligibilityService.createEvaluationForOfficer(30L, duplicateRequest))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Duplicate loan type");
		verify(incomeRepository, never()).findAllByFinancialRecord_BankRecordId(any());
	}

	private void configureOfficerAndCustomerContext() {
		User officerUser = new User();
		officerUser.setUserId(22L);
		officerUser.setUsername("officer");

		BankOfficer officer = new BankOfficer();
		officer.setOfficerId(5L);
		officer.setUser(officerUser);

		User customerUser = new User();
		customerUser.setUserId(12L);
		customerUser.setDob(LocalDate.of(1990, 1, 1));

		customer = new BankCustomer();
		customer.setBankCustomerId(30L);
		customer.setCustomerCode("BC-00030");
		customer.setUser(customerUser);

		financialRecord = new BankCustomerFinancialRecord();
		financialRecord.setBankRecordId(40L);
		financialRecord.setBankCustomer(customer);

		BankCreditEvaluation creditEvaluation = new BankCreditEvaluation();
		creditEvaluation.setBankEvaluationId(50L);
		creditEvaluation.setRiskLevel("LOW");

		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("officer", null, List.of())
		);
		when(userRepository.findByEmail("officer")).thenReturn(Optional.empty());
		when(userRepository.findByUsername("officer")).thenReturn(Optional.of(officerUser));
		when(bankOfficerRepository.findByUser_UserId(22L)).thenReturn(Optional.of(officer));
		when(bankCustomerRepository.findById(30L)).thenReturn(Optional.of(customer));
		when(financialRecordRepository.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(30L))
			.thenReturn(Optional.of(financialRecord));
		when(creditEvaluationService.getOrCreateLatestBankEvaluationForCustomer(customer)).thenReturn(creditEvaluation);
		when(loanEligibilityRepository.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(30L))
			.thenReturn(Optional.empty());
	}

	private void stubFinancialInputs(BigDecimal monthlyIncome, BigDecimal existingLoanEmi) {
		BankCustomerIncome income = new BankCustomerIncome();
		income.setFinancialRecord(financialRecord);
		income.setIncomeCategory("SALARY");
		income.setAmount(monthlyIncome);
		when(incomeRepository.findAllByFinancialRecord_BankRecordId(40L)).thenReturn(List.of(income));

		if (existingLoanEmi.compareTo(BigDecimal.ZERO) > 0) {
			BankCustomerLoan loan = new BankCustomerLoan();
			loan.setFinancialRecord(financialRecord);
			loan.setMonthlyEmi(existingLoanEmi);
			when(loanRepository.findAllByFinancialRecord_BankRecordId(40L)).thenReturn(List.of(loan));
		} else {
			when(loanRepository.findAllByFinancialRecord_BankRecordId(40L)).thenReturn(List.of());
		}
		when(cardRepository.findAllByFinancialRecord_BankRecordId(40L)).thenReturn(List.of());
		when(liabilityRepository.findAllByFinancialRecord_BankRecordId(40L)).thenReturn(List.of());
		when(missedPaymentRepository.findByFinancialRecord_BankRecordId(40L)).thenReturn(Optional.empty());
	}

	private void stubPolicyAndRisk() {
		LoanPolicy policy = new LoanPolicy();
		policy.setPolicyId(1L);
		policy.setLoanType("PERSONAL");
		policy.setMaxDbrRatio(new BigDecimal("0.40"));
		policy.setBaseInterestRate(new BigDecimal("10.00"));
		policy.setMaxTenureMonths(60);
		policy.setMinAge(21);
		policy.setMaxAge(60);
		policy.setMinIncomeRequired(new BigDecimal("50000.00"));
		policy.setStatus("ACTIVE");

		RiskAdjustment adjustment = new RiskAdjustment();
		adjustment.setRiskLevel("LOW");
		adjustment.setMultiplier(new BigDecimal("1.00"));
		adjustment.setDescription("Low-risk customer.");

		when(loanPolicyRepository.findAllByStatusOrderByLoanTypeAsc("ACTIVE")).thenReturn(List.of(policy));
		when(riskAdjustmentRepository.findAllByOrderByRiskLevelAsc()).thenReturn(List.of(adjustment));
	}

	private void stubEvaluationSave() {
		when(loanEligibilityRepository.save(any(LoanSenseEvaluation.class))).thenAnswer(invocation -> {
			LoanSenseEvaluation evaluation = invocation.getArgument(0);
			evaluation.setLoansenseEvaluationId(60L);
			return evaluation;
		});
		when(loanEligibilityMapper.toEvaluationResponse(any(LoanSenseEvaluation.class))).thenReturn(mappedResponse);
	}

	private LoanSenseEvaluation capturedEvaluation() {
		ArgumentCaptor<LoanSenseEvaluation> captor = ArgumentCaptor.forClass(LoanSenseEvaluation.class);
		verify(loanEligibilityRepository).save(captor.capture());
		return captor.getValue();
	}

	private CreateLoanSenseEvaluationRequest request(String loanType) {
		return new CreateLoanSenseEvaluationRequest(List.of(new LoanSenseLoanInputRequest(loanType, null)));
	}
}
