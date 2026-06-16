package com.bank_web_app.backend.creditlens.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLiability;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerMissedPayment;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerIncomeRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLiabilityRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLoanRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerMissedPaymentRepository;
import com.bank_web_app.backend.creditlens.entity.BankCreditEvaluation;
import com.bank_web_app.backend.creditlens.entity.SelfCreditEvaluation;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationAmounts.estimateCardMinimumPayment;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationAmounts.safeAmount;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationText.normalizeText;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerMissedPayment;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerCardRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerIncomeRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLiabilityRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLoanRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerMissedPaymentRepository;

@Service
public class CreditEvaluationScoringService {

	private static final int LOW_RISK_MAX_POINTS = 33;
	private static final int MEDIUM_RISK_MAX_POINTS = 66;

	private final PublicCustomerIncomeRepository publicCustomerIncomeRepository;
	private final PublicCustomerLoanRepository publicCustomerLoanRepository;
	private final PublicCustomerCardRepository publicCustomerCardRepository;
	private final PublicCustomerLiabilityRepository publicCustomerLiabilityRepository;
	private final PublicCustomerMissedPaymentRepository publicCustomerMissedPaymentRepository;
	private final BankCustomerIncomeRepository bankCustomerIncomeRepository;
	private final BankCustomerLoanRepository bankCustomerLoanRepository;
	private final BankCustomerCardRepository bankCustomerCardRepository;
	private final BankCustomerLiabilityRepository bankCustomerLiabilityRepository;
	private final BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository;

	// Wires all financial repositories needed to calculate CreditLens scores.
	public CreditEvaluationScoringService(
		PublicCustomerIncomeRepository publicCustomerIncomeRepository,
		PublicCustomerLoanRepository publicCustomerLoanRepository,
		PublicCustomerCardRepository publicCustomerCardRepository,
		PublicCustomerLiabilityRepository publicCustomerLiabilityRepository,
		PublicCustomerMissedPaymentRepository publicCustomerMissedPaymentRepository,
		BankCustomerIncomeRepository bankCustomerIncomeRepository,
		BankCustomerLoanRepository bankCustomerLoanRepository,
		BankCustomerCardRepository bankCustomerCardRepository,
		BankCustomerLiabilityRepository bankCustomerLiabilityRepository,
		BankCustomerMissedPaymentRepository bankCustomerMissedPaymentRepository
	) {
		this.publicCustomerIncomeRepository = publicCustomerIncomeRepository;
		this.publicCustomerLoanRepository = publicCustomerLoanRepository;
		this.publicCustomerCardRepository = publicCustomerCardRepository;
		this.publicCustomerLiabilityRepository = publicCustomerLiabilityRepository;
		this.publicCustomerMissedPaymentRepository = publicCustomerMissedPaymentRepository;
		this.bankCustomerIncomeRepository = bankCustomerIncomeRepository;
		this.bankCustomerLoanRepository = bankCustomerLoanRepository;
		this.bankCustomerCardRepository = bankCustomerCardRepository;
		this.bankCustomerLiabilityRepository = bankCustomerLiabilityRepository;
		this.bankCustomerMissedPaymentRepository = bankCustomerMissedPaymentRepository;
	}

	// Builds the complete CreditLens metrics from public-customer financial data.
	EvaluationMetrics buildPublicEvaluationMetrics(PublicCustomerFinancialRecord record) {
		Long recordId = record.getRecordId();
		List<PublicCustomerIncome> incomes = publicCustomerIncomeRepository.findAllByFinancialRecord_RecordId(recordId);
		List<PublicCustomerLoan> loans = publicCustomerLoanRepository.findAllByFinancialRecord_RecordId(recordId);
		List<PublicCustomerCard> cards = publicCustomerCardRepository.findAllByFinancialRecord_RecordId(recordId);
		List<PublicCustomerLiability> liabilities = publicCustomerLiabilityRepository.findAllByFinancialRecord_RecordId(recordId);
		int missedPaymentsCount = publicCustomerMissedPaymentRepository
			.findByFinancialRecord_RecordId(recordId)
			.map(PublicCustomerMissedPayment::getMissedPayments)
			.orElse(0);
		validatePublicFinancialInputs(incomes, loans, cards, liabilities, missedPaymentsCount);

		BigDecimal totalMonthlyIncome = incomes.stream()
			.map(PublicCustomerIncome::getAmount)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardLimit = cards.stream()
			.map(PublicCustomerCard::getCreditLimit)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardOutstanding = cards.stream()
			.map(PublicCustomerCard::getOutstandingBalance)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalMonthlyDebtPayment = loans.stream()
			.map(PublicCustomerLoan::getMonthlyEmi)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(liabilities.stream()
				.map(PublicCustomerLiability::getMonthlyAmount)
				.map(CreditEvaluationAmounts::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add))
			.add(estimateCardMinimumPayment(totalCardOutstanding));
		int activeFacilitiesCount = loans.size() + cards.size() + liabilities.size();
		int incomeStabilityPoints = calculateIncomeStabilityPointsForPublic(incomes);

		return calculateEvaluationMetrics(
			totalMonthlyIncome,
			totalMonthlyDebtPayment,
			totalCardLimit,
			totalCardOutstanding,
			activeFacilitiesCount,
			missedPaymentsCount,
			incomeStabilityPoints
		);
	}

	// Builds the complete CreditLens metrics from bank-customer financial data.
	EvaluationMetrics buildBankEvaluationMetrics(BankCustomerFinancialRecord record) {
		Long bankRecordId = record.getBankRecordId();
		List<BankCustomerIncome> incomes = bankCustomerIncomeRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerLoan> loans = bankCustomerLoanRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerCard> cards = bankCustomerCardRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		List<BankCustomerLiability> liabilities = bankCustomerLiabilityRepository.findAllByFinancialRecord_BankRecordId(bankRecordId);
		int missedPaymentsCount = bankCustomerMissedPaymentRepository
			.findByFinancialRecord_BankRecordId(bankRecordId)
			.map(BankCustomerMissedPayment::getMissedPayments)
			.orElse(0);
		validateBankFinancialInputs(incomes, loans, cards, liabilities, missedPaymentsCount);

		BigDecimal totalMonthlyIncome = incomes.stream()
			.map(BankCustomerIncome::getAmount)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardLimit = cards.stream()
			.map(BankCustomerCard::getCreditLimit)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardOutstanding = cards.stream()
			.map(BankCustomerCard::getOutstandingBalance)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalMonthlyDebtPayment = loans.stream()
			.map(BankCustomerLoan::getMonthlyEmi)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(liabilities.stream()
				.map(BankCustomerLiability::getMonthlyAmount)
				.map(CreditEvaluationAmounts::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add))
			.add(estimateCardMinimumPayment(totalCardOutstanding));
		int activeFacilitiesCount = loans.size() + cards.size() + liabilities.size();
		int incomeStabilityPoints = calculateIncomeStabilityPointsForBank(incomes);

		return calculateEvaluationMetrics(
			totalMonthlyIncome,
			totalMonthlyDebtPayment,
			totalCardLimit,
			totalCardOutstanding,
			activeFacilitiesCount,
			missedPaymentsCount,
			incomeStabilityPoints
		);
	}

	// Copies calculated metrics into a self credit evaluation entity.
	void applyCommonMetricsToSelfEvaluation(SelfCreditEvaluation evaluation, EvaluationMetrics metrics) {
		evaluation.setTotalRiskPoints(metrics.totalRiskPoints());
		evaluation.setRiskLevel(metrics.riskLevel());
		evaluation.setTotalMonthlyIncome(metrics.totalMonthlyIncome());
		evaluation.setTotalMonthlyDebtPayment(metrics.totalMonthlyDebtPayment());
		evaluation.setTotalCardLimit(metrics.totalCardLimit());
		evaluation.setTotalCardOutstanding(metrics.totalCardOutstanding());
		evaluation.setDtiRatio(metrics.dtiRatio());
		evaluation.setCreditUtilizationRatio(metrics.creditUtilizationRatio());
		evaluation.setActiveFacilitiesCount(metrics.activeFacilitiesCount());
		evaluation.setMissedPaymentsCount(metrics.missedPaymentsCount());
		evaluation.setPaymentHistoryPoints(metrics.paymentHistoryPoints());
		evaluation.setDtiPoints(metrics.dtiPoints());
		evaluation.setUtilizationPoints(metrics.utilizationPoints());
		evaluation.setIncomeStabilityPoints(metrics.incomeStabilityPoints());
		evaluation.setExposurePoints(metrics.exposurePoints());
		evaluation.setReportGenerated(Boolean.FALSE);
	}

	// Copies calculated metrics into a bank credit evaluation entity.
	void applyCommonMetricsToBankEvaluation(BankCreditEvaluation evaluation, EvaluationMetrics metrics) {
		evaluation.setTotalRiskPoints(metrics.totalRiskPoints());
		evaluation.setRiskLevel(metrics.riskLevel());
		evaluation.setTotalMonthlyIncome(metrics.totalMonthlyIncome());
		evaluation.setTotalMonthlyDebtPayment(metrics.totalMonthlyDebtPayment());
		evaluation.setTotalCardLimit(metrics.totalCardLimit());
		evaluation.setTotalCardOutstanding(metrics.totalCardOutstanding());
		evaluation.setDtiRatio(metrics.dtiRatio());
		evaluation.setCreditUtilizationRatio(metrics.creditUtilizationRatio());
		evaluation.setActiveFacilitiesCount(metrics.activeFacilitiesCount());
		evaluation.setMissedPaymentsCount(metrics.missedPaymentsCount());
		evaluation.setPaymentHistoryPoints(metrics.paymentHistoryPoints());
		evaluation.setDtiPoints(metrics.dtiPoints());
		evaluation.setUtilizationPoints(metrics.utilizationPoints());
		evaluation.setIncomeStabilityPoints(metrics.incomeStabilityPoints());
		evaluation.setExposurePoints(metrics.exposurePoints());
		evaluation.setReportGenerated(Boolean.FALSE);
	}

	// Checks whether a stored self evaluation still matches current metrics.
	boolean matchesSelfEvaluationMetrics(SelfCreditEvaluation evaluation, EvaluationMetrics metrics) {
		return
			Objects.equals(evaluation.getTotalRiskPoints(), metrics.totalRiskPoints()) &&
			Objects.equals(evaluation.getRiskLevel(), metrics.riskLevel()) &&
			isSameAmount(evaluation.getTotalMonthlyIncome(), metrics.totalMonthlyIncome()) &&
			isSameAmount(evaluation.getTotalMonthlyDebtPayment(), metrics.totalMonthlyDebtPayment()) &&
			isSameAmount(evaluation.getTotalCardLimit(), metrics.totalCardLimit()) &&
			isSameAmount(evaluation.getTotalCardOutstanding(), metrics.totalCardOutstanding()) &&
			isSameAmount(evaluation.getDtiRatio(), metrics.dtiRatio()) &&
			isSameAmount(evaluation.getCreditUtilizationRatio(), metrics.creditUtilizationRatio()) &&
			Objects.equals(evaluation.getActiveFacilitiesCount(), metrics.activeFacilitiesCount()) &&
			Objects.equals(evaluation.getMissedPaymentsCount(), metrics.missedPaymentsCount()) &&
			Objects.equals(evaluation.getPaymentHistoryPoints(), metrics.paymentHistoryPoints()) &&
			Objects.equals(evaluation.getDtiPoints(), metrics.dtiPoints()) &&
			Objects.equals(evaluation.getUtilizationPoints(), metrics.utilizationPoints()) &&
			Objects.equals(evaluation.getIncomeStabilityPoints(), metrics.incomeStabilityPoints()) &&
			Objects.equals(evaluation.getExposurePoints(), metrics.exposurePoints());
	}

	// Checks whether a stored bank evaluation still matches current metrics.
	boolean matchesBankEvaluationMetrics(BankCreditEvaluation evaluation, EvaluationMetrics metrics) {
		return
			Objects.equals(evaluation.getTotalRiskPoints(), metrics.totalRiskPoints()) &&
			Objects.equals(evaluation.getRiskLevel(), metrics.riskLevel()) &&
			isSameAmount(evaluation.getTotalMonthlyIncome(), metrics.totalMonthlyIncome()) &&
			isSameAmount(evaluation.getTotalMonthlyDebtPayment(), metrics.totalMonthlyDebtPayment()) &&
			isSameAmount(evaluation.getTotalCardLimit(), metrics.totalCardLimit()) &&
			isSameAmount(evaluation.getTotalCardOutstanding(), metrics.totalCardOutstanding()) &&
			isSameAmount(evaluation.getDtiRatio(), metrics.dtiRatio()) &&
			isSameAmount(evaluation.getCreditUtilizationRatio(), metrics.creditUtilizationRatio()) &&
			Objects.equals(evaluation.getActiveFacilitiesCount(), metrics.activeFacilitiesCount()) &&
			Objects.equals(evaluation.getMissedPaymentsCount(), metrics.missedPaymentsCount()) &&
			Objects.equals(evaluation.getPaymentHistoryPoints(), metrics.paymentHistoryPoints()) &&
			Objects.equals(evaluation.getDtiPoints(), metrics.dtiPoints()) &&
			Objects.equals(evaluation.getUtilizationPoints(), metrics.utilizationPoints()) &&
			Objects.equals(evaluation.getIncomeStabilityPoints(), metrics.incomeStabilityPoints()) &&
			Objects.equals(evaluation.getExposurePoints(), metrics.exposurePoints());
	}

	// Calculates ratios, factor points, total risk points, and risk level.
	private EvaluationMetrics calculateEvaluationMetrics(
		BigDecimal totalMonthlyIncome,
		BigDecimal totalMonthlyDebtPayment,
		BigDecimal totalCardLimit,
		BigDecimal totalCardOutstanding,
		int activeFacilitiesCount,
		int missedPaymentsCount,
		int incomeStabilityPoints
	) {
		if (totalMonthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("A positive monthly income is required before generating a credit evaluation.");
		}

		BigDecimal dtiRatio = totalMonthlyDebtPayment
			.divide(totalMonthlyIncome, 4, RoundingMode.HALF_UP);
		BigDecimal creditUtilizationRatio = totalCardLimit.compareTo(BigDecimal.ZERO) <= 0
			? (totalCardOutstanding.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO)
			: totalCardOutstanding.divide(totalCardLimit, 4, RoundingMode.HALF_UP);

		int paymentHistoryPoints = calculatePaymentHistoryPoints(missedPaymentsCount);
		int dtiPoints = calculateDtiPoints(dtiRatio);
		int utilizationPoints = calculateUtilizationPoints(creditUtilizationRatio);
		int exposurePoints = calculateExposurePoints(activeFacilitiesCount);
		int totalRiskPoints = paymentHistoryPoints + dtiPoints + utilizationPoints + incomeStabilityPoints + exposurePoints;

		return new EvaluationMetrics(
			totalRiskPoints,
			resolveRiskLevel(totalRiskPoints),
			totalMonthlyIncome.setScale(2, RoundingMode.HALF_UP),
			totalMonthlyDebtPayment.setScale(2, RoundingMode.HALF_UP),
			totalCardLimit.setScale(2, RoundingMode.HALF_UP),
			totalCardOutstanding.setScale(2, RoundingMode.HALF_UP),
			dtiRatio,
			creditUtilizationRatio,
			activeFacilitiesCount,
			missedPaymentsCount,
			paymentHistoryPoints,
			dtiPoints,
			utilizationPoints,
			incomeStabilityPoints,
			exposurePoints
		);
	}

	// Converts missed payment count into payment history risk points.
	private int calculatePaymentHistoryPoints(int missedPaymentsCount) {
		if (missedPaymentsCount <= 0) {
			return 0;
		}
		if (missedPaymentsCount == 1) {
			return 8;
		}
		if (missedPaymentsCount <= 3) {
			return 18;
		}
		return 30;
	}

	// Converts debt-to-income ratio into DTI risk points.
	private int calculateDtiPoints(BigDecimal dtiRatio) {
		if (dtiRatio.compareTo(new BigDecimal("0.30")) <= 0) {
			return 0;
		}
		if (dtiRatio.compareTo(new BigDecimal("0.50")) <= 0) {
			return 12;
		}
		return 25;
	}

	// Converts card utilization ratio into utilization risk points.
	private int calculateUtilizationPoints(BigDecimal creditUtilizationRatio) {
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.40")) <= 0) {
			return 0;
		}
		if (creditUtilizationRatio.compareTo(new BigDecimal("0.70")) <= 0) {
			return 10;
		}
		return 20;
	}

	// Converts active facility count into exposure risk points.
	private int calculateExposurePoints(int activeFacilitiesCount) {
		if (activeFacilitiesCount <= 2) {
			return 0;
		}
		if (activeFacilitiesCount <= 4) {
			return 5;
		}
		return 10;
	}

	// Calculates public-customer income stability points across all income rows.
	private int calculateIncomeStabilityPointsForPublic(List<PublicCustomerIncome> incomes) {
		if (incomes.isEmpty()) {
			return 15;
		}
		BigDecimal share = BigDecimal.valueOf(15)
			.divide(BigDecimal.valueOf(incomes.size()), 4, RoundingMode.HALF_UP);
		BigDecimal totalPoints = incomes.stream()
			.map(income -> share.multiply(resolveIncomeRiskMultiplier(
				income.getIncomeCategory(),
				income.getEmploymentType(),
				income.getDurationMonths(),
				income.getIncomeStability()
			)))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return totalPoints.setScale(0, RoundingMode.HALF_UP).intValue();
	}

	// Calculates bank-customer income stability points across all income rows.
	private int calculateIncomeStabilityPointsForBank(List<BankCustomerIncome> incomes) {
		if (incomes.isEmpty()) {
			return 15;
		}
		BigDecimal share = BigDecimal.valueOf(15)
			.divide(BigDecimal.valueOf(incomes.size()), 4, RoundingMode.HALF_UP);
		BigDecimal totalPoints = incomes.stream()
			.map(income -> share.multiply(resolveIncomeRiskMultiplier(
				income.getIncomeCategory(),
				income.getEmploymentType(),
				income.getDurationMonths(),
				income.getIncomeStability()
			)))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return totalPoints.setScale(0, RoundingMode.HALF_UP).intValue();
	}

	// Resolves how risky one income source is based on category and stability.
	private BigDecimal resolveIncomeRiskMultiplier(
		String incomeCategory,
		String employmentType,
		Integer durationMonths,
		String incomeStability
	) {
		String normalizedCategory = normalizeText(incomeCategory);
		String normalizedEmploymentType = normalizeText(employmentType);
		String normalizedIncomeStability = normalizeText(incomeStability);
		int normalizedDurationMonths = durationMonths == null ? 0 : durationMonths;

		if ("SALARY".equals(normalizedCategory)) {
			if (normalizedEmploymentType.contains("PERMANENT")) {
				if (normalizedDurationMonths > 12) {
					return BigDecimal.ZERO;
				}
				if (normalizedDurationMonths >= 6) {
					return new BigDecimal("0.5");
				}
				return BigDecimal.ONE;
			}
			if (normalizedEmploymentType.contains("CONTRACT")) {
				return normalizedDurationMonths > 0 && normalizedDurationMonths < 6 ? BigDecimal.ONE : new BigDecimal("0.5");
			}
			return BigDecimal.ONE;
		}

		if ("BUSINESS".equals(normalizedCategory)) {
			if ("STABLE".equals(normalizedIncomeStability)) {
				return BigDecimal.ZERO;
			}
			if (
				normalizedIncomeStability.contains("MEDIUM") ||
				normalizedIncomeStability.contains("MODERATE")
			) {
				return new BigDecimal("0.5");
			}
			return BigDecimal.ONE;
		}

		return BigDecimal.ONE;
	}

	// Converts total risk points into LOW, MEDIUM, or HIGH risk level.
	private String resolveRiskLevel(int totalRiskPoints) {
		if (totalRiskPoints <= LOW_RISK_MAX_POINTS) {
			return "LOW";
		}
		if (totalRiskPoints <= MEDIUM_RISK_MAX_POINTS) {
			return "MEDIUM";
		}
		return "HIGH";
	}

	// Compares two money values after converting nulls to zero.
	private boolean isSameAmount(BigDecimal left, BigDecimal right) {
		return safeAmount(left).compareTo(safeAmount(right)) == 0;
	}

	// Validates public-customer financial inputs before scoring.
	private void validatePublicFinancialInputs(
		List<PublicCustomerIncome> incomes,
		List<PublicCustomerLoan> loans,
		List<PublicCustomerCard> cards,
		List<PublicCustomerLiability> liabilities,
		int missedPaymentsCount
	) {
		incomes.forEach(income -> {
			if (safeAmount(income.getAmount()).compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Each public-customer income must be greater than 0 before calculating CreditLens.");
			}
		});
		loans.forEach(loan -> {
			if (safeAmount(loan.getMonthlyEmi()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Public-customer loan EMI values must not be negative before calculating CreditLens.");
			}
		});
		cards.forEach(card -> {
			if (safeAmount(card.getCreditLimit()).compareTo(BigDecimal.ZERO) < 0 || safeAmount(card.getOutstandingBalance()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Public-customer credit-card limits and balances must not be negative before calculating CreditLens.");
			}
		});
		liabilities.forEach(liability -> {
			if (safeAmount(liability.getMonthlyAmount()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Public-customer liability values must not be negative before calculating CreditLens.");
			}
		});
		if (missedPaymentsCount < 0) {
			throw new IllegalArgumentException("Public-customer missed payments must not be negative before calculating CreditLens.");
		}
	}

	// Validates bank-customer financial inputs before scoring.
	private void validateBankFinancialInputs(
		List<BankCustomerIncome> incomes,
		List<BankCustomerLoan> loans,
		List<BankCustomerCard> cards,
		List<BankCustomerLiability> liabilities,
		int missedPaymentsCount
	) {
		incomes.forEach(income -> {
			if (safeAmount(income.getAmount()).compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Each bank-customer income must be greater than 0 before calculating CreditLens.");
			}
		});
		loans.forEach(loan -> {
			if (safeAmount(loan.getMonthlyEmi()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Bank-customer loan EMI values must not be negative before calculating CreditLens.");
			}
		});
		cards.forEach(card -> {
			if (safeAmount(card.getCreditLimit()).compareTo(BigDecimal.ZERO) < 0 || safeAmount(card.getOutstandingBalance()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Bank-customer credit-card limits and balances must not be negative before calculating CreditLens.");
			}
		});
		liabilities.forEach(liability -> {
			if (safeAmount(liability.getMonthlyAmount()).compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Bank-customer liability values must not be negative before calculating CreditLens.");
			}
		});
		if (missedPaymentsCount < 0) {
			throw new IllegalArgumentException("Bank-customer missed payments must not be negative before calculating CreditLens.");
		}
	}
}
