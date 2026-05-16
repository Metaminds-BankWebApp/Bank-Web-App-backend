package com.bank_web_app.backend.creditlens.service;

import static com.bank_web_app.backend.creditlens.service.CreditEvaluationAmounts.estimateCardMinimumPayment;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationAmounts.safeAmount;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLiability;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerCardRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerFinancialRecordRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerIncomeRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLiabilityRepository;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerLoanRepository;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerCardRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerFinancialRecordRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerIncomeRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLiabilityRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLoanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CreditEvaluationRecordService {

	private final PublicCustomerFinancialRecordRepository publicCustomerFinancialRecordRepository;
	private final PublicCustomerIncomeRepository publicCustomerIncomeRepository;
	private final PublicCustomerLoanRepository publicCustomerLoanRepository;
	private final PublicCustomerCardRepository publicCustomerCardRepository;
	private final PublicCustomerLiabilityRepository publicCustomerLiabilityRepository;
	private final BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository;
	private final BankCustomerIncomeRepository bankCustomerIncomeRepository;
	private final BankCustomerLoanRepository bankCustomerLoanRepository;
	private final BankCustomerCardRepository bankCustomerCardRepository;
	private final BankCustomerLiabilityRepository bankCustomerLiabilityRepository;

	// Wires the public and bank financial repositories used for CreditLens records.
	public CreditEvaluationRecordService(
		PublicCustomerFinancialRecordRepository publicCustomerFinancialRecordRepository,
		PublicCustomerIncomeRepository publicCustomerIncomeRepository,
		PublicCustomerLoanRepository publicCustomerLoanRepository,
		PublicCustomerCardRepository publicCustomerCardRepository,
		PublicCustomerLiabilityRepository publicCustomerLiabilityRepository,
		BankCustomerFinancialRecordRepository bankCustomerFinancialRecordRepository,
		BankCustomerIncomeRepository bankCustomerIncomeRepository,
		BankCustomerLoanRepository bankCustomerLoanRepository,
		BankCustomerCardRepository bankCustomerCardRepository,
		BankCustomerLiabilityRepository bankCustomerLiabilityRepository
	) {
		this.publicCustomerFinancialRecordRepository = publicCustomerFinancialRecordRepository;
		this.publicCustomerIncomeRepository = publicCustomerIncomeRepository;
		this.publicCustomerLoanRepository = publicCustomerLoanRepository;
		this.publicCustomerCardRepository = publicCustomerCardRepository;
		this.publicCustomerLiabilityRepository = publicCustomerLiabilityRepository;
		this.bankCustomerFinancialRecordRepository = bankCustomerFinancialRecordRepository;
		this.bankCustomerIncomeRepository = bankCustomerIncomeRepository;
		this.bankCustomerLoanRepository = bankCustomerLoanRepository;
		this.bankCustomerCardRepository = bankCustomerCardRepository;
		this.bankCustomerLiabilityRepository = bankCustomerLiabilityRepository;
	}

	// Finds the current public-customer financial record used for self evaluation.
	PublicCustomerFinancialRecord resolveCurrentPublicFinancialRecord(Long publicCustomerId) {
		return publicCustomerFinancialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.orElseThrow(() -> new IllegalArgumentException("No current financial record found for this public customer."));
	}

	// Finds the newest bank-customer financial record used for bank evaluation.
	BankCustomerFinancialRecord resolveLatestBankFinancialRecord(Long bankCustomerId) {
		return bankCustomerFinancialRecordRepository
			.findTopByBankCustomer_BankCustomerIdOrderByCreatedAtDesc(bankCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("No financial record found for this bank customer."));
	}

	// Checks whether stored financial data changed after an evaluation was created.
	boolean isRecordUpdatedAfterEvaluation(LocalDateTime recordUpdatedAt, LocalDateTime evaluationCreatedAt) {
		return recordUpdatedAt != null && evaluationCreatedAt != null && recordUpdatedAt.isAfter(evaluationCreatedAt);
	}

	// Loads income, loans, cards, and liabilities for the report breakdown.
	RecordBreakdown loadRecordBreakdown(EvaluationView view) {
		if ("PUBLIC".equals(view.scope())) {
			List<PublicCustomerIncome> incomes = publicCustomerIncomeRepository.findAllByFinancialRecord_RecordId(view.recordId());
			List<PublicCustomerLoan> loans = publicCustomerLoanRepository.findAllByFinancialRecord_RecordId(view.recordId());
			List<PublicCustomerCard> cards = publicCustomerCardRepository.findAllByFinancialRecord_RecordId(view.recordId());
			List<PublicCustomerLiability> liabilities = publicCustomerLiabilityRepository.findAllByFinancialRecord_RecordId(view.recordId());
			BigDecimal income = incomes.stream().map(PublicCustomerIncome::getAmount).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal loanEmi = loans.stream().map(PublicCustomerLoan::getMonthlyEmi).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal cardBalance = cards.stream().map(PublicCustomerCard::getOutstandingBalance).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal cardLimit = cards.stream().map(PublicCustomerCard::getCreditLimit).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal liabilitiesTotal = liabilities.stream().map(PublicCustomerLiability::getMonthlyAmount).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			return new RecordBreakdown(
				income.setScale(2, RoundingMode.HALF_UP),
				loanEmi.setScale(2, RoundingMode.HALF_UP),
				cardBalance.setScale(2, RoundingMode.HALF_UP),
				cardLimit.setScale(2, RoundingMode.HALF_UP),
				liabilitiesTotal.setScale(2, RoundingMode.HALF_UP),
				estimateCardMinimumPayment(cardBalance)
			);
		}

		List<BankCustomerIncome> incomes = bankCustomerIncomeRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		List<BankCustomerLoan> loans = bankCustomerLoanRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		List<BankCustomerCard> cards = bankCustomerCardRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		List<BankCustomerLiability> liabilities = bankCustomerLiabilityRepository.findAllByFinancialRecord_BankRecordId(view.recordId());
		BigDecimal income = incomes.stream().map(BankCustomerIncome::getAmount).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal loanEmi = loans.stream().map(BankCustomerLoan::getMonthlyEmi).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal cardBalance = cards.stream().map(BankCustomerCard::getOutstandingBalance).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal cardLimit = cards.stream().map(BankCustomerCard::getCreditLimit).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal liabilitiesTotal = liabilities.stream().map(BankCustomerLiability::getMonthlyAmount).map(CreditEvaluationAmounts::safeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new RecordBreakdown(
			income.setScale(2, RoundingMode.HALF_UP),
			loanEmi.setScale(2, RoundingMode.HALF_UP),
			cardBalance.setScale(2, RoundingMode.HALF_UP),
			cardLimit.setScale(2, RoundingMode.HALF_UP),
			liabilitiesTotal.setScale(2, RoundingMode.HALF_UP),
			estimateCardMinimumPayment(cardBalance)
		);
	}

	// Adds all remaining loan balances for a public-customer financial record.
	BigDecimal loadPublicLoanRemainingBalance(Long recordId) {
		return publicCustomerLoanRepository.findAllByFinancialRecord_RecordId(recordId)
			.stream()
			.map(PublicCustomerLoan::getRemainingBalance)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.setScale(2, RoundingMode.HALF_UP);
	}

	// Adds all remaining loan balances for a bank-customer financial record.
	BigDecimal loadBankLoanRemainingBalance(Long bankRecordId) {
		return bankCustomerLoanRepository.findAllByFinancialRecord_BankRecordId(bankRecordId)
			.stream()
			.map(BankCustomerLoan::getRemainingBalance)
			.map(CreditEvaluationAmounts::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.setScale(2, RoundingMode.HALF_UP);
	}
}
