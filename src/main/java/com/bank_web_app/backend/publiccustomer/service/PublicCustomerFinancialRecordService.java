package com.bank_web_app.backend.publiccustomer.service;

import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerCardStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerIncomeStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLiabilityStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.request.PublicCustomerLoanStepRequest;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialStepResponse;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerMissedPayment;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerProfile;
import com.bank_web_app.backend.publiccustomer.mapper.PublicCustomerFinancialRecordMapper;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerCardRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerFinancialRecordRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerIncomeRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLiabilityRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerLoanRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerMissedPaymentRepository;
import com.bank_web_app.backend.publiccustomer.repository.PublicCustomerProfileRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicCustomerFinancialRecordService {

	private final PublicCustomerProfileRepository publicCustomerProfileRepository;
	private final PublicCustomerFinancialRecordRepository financialRecordRepository;
	private final PublicCustomerIncomeRepository incomeRepository;
	private final PublicCustomerLoanRepository loanRepository;
	private final PublicCustomerCardRepository cardRepository;
	private final PublicCustomerLiabilityRepository liabilityRepository;
	private final PublicCustomerMissedPaymentRepository missedPaymentRepository;
	private final PublicCustomerFinancialRecordMapper financialRecordMapper;

	public PublicCustomerFinancialRecordService(
		PublicCustomerProfileRepository publicCustomerProfileRepository,
		PublicCustomerFinancialRecordRepository financialRecordRepository,
		PublicCustomerIncomeRepository incomeRepository,
		PublicCustomerLoanRepository loanRepository,
		PublicCustomerCardRepository cardRepository,
		PublicCustomerLiabilityRepository liabilityRepository,
		PublicCustomerMissedPaymentRepository missedPaymentRepository,
		PublicCustomerFinancialRecordMapper financialRecordMapper
	) {
		this.publicCustomerProfileRepository = publicCustomerProfileRepository;
		this.financialRecordRepository = financialRecordRepository;
		this.incomeRepository = incomeRepository;
		this.loanRepository = loanRepository;
		this.cardRepository = cardRepository;
		this.liabilityRepository = liabilityRepository;
		this.missedPaymentRepository = missedPaymentRepository;
		this.financialRecordMapper = financialRecordMapper;
	}

	@Transactional
	public PublicCustomerFinancialStepResponse saveIncomeStep(Long publicCustomerId, PublicCustomerIncomeStepRequest request) {
		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		incomeRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerIncomeStepRequest.IncomeItem incomeItem : request.incomes()) {
			PublicCustomerIncome income = new PublicCustomerIncome();
			income.setFinancialRecord(currentRecord);
			income.setIncomeCategory(normalizeIncomeCategory(incomeItem.incomeCategory()));
			income.setAmount(incomeItem.amount());
			income.setSalaryType(incomeItem.salaryType());
			income.setEmploymentType(incomeItem.employmentType());
			income.setContractDurationMonths(incomeItem.contractDurationMonths());
			income.setIncomeStability(incomeItem.incomeStability());
			incomeRepository.save(income);
		}

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "INCOME", "Income step saved successfully.");
	}

	@Transactional
	public PublicCustomerFinancialStepResponse saveLoanStep(Long publicCustomerId, PublicCustomerLoanStepRequest request) {
		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		loanRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerLoanStepRequest.LoanItem loanItem : request.loans()) {
			PublicCustomerLoan loan = new PublicCustomerLoan();
			loan.setFinancialRecord(currentRecord);
			loan.setLoanType(loanItem.loanType());
			loan.setMonthlyEmi(loanItem.monthlyEmi());
			loan.setRemainingBalance(loanItem.remainingBalance());
			loanRepository.save(loan);
		}

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "LOANS", "Loan step saved successfully.");
	}

	@Transactional
	public PublicCustomerFinancialStepResponse saveCardStep(Long publicCustomerId, PublicCustomerCardStepRequest request) {
		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		cardRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerCardStepRequest.CardItem cardItem : request.cards()) {
			PublicCustomerCard card = new PublicCustomerCard();
			card.setFinancialRecord(currentRecord);
			card.setProvider(cardItem.provider());
			card.setCreditLimit(cardItem.creditLimit());
			card.setOutstandingBalance(cardItem.outstandingBalance());
			cardRepository.save(card);
		}

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(recordId, publicCustomerId, "CARDS", "Card step saved successfully.");
	}

	@Transactional
	public PublicCustomerFinancialStepResponse saveLiabilityStep(Long publicCustomerId, PublicCustomerLiabilityStepRequest request) {
		PublicCustomerFinancialRecord currentRecord = getOrCreateCurrentRecord(publicCustomerId);
		Long recordId = currentRecord.getRecordId();

		liabilityRepository.deleteByFinancialRecord_RecordId(recordId);

		for (PublicCustomerLiabilityStepRequest.LiabilityItem liabilityItem : request.liabilities()) {
			PublicCustomerLiability liability = new PublicCustomerLiability();
			liability.setFinancialRecord(currentRecord);
			liability.setDescription(liabilityItem.description());
			liability.setMonthlyAmount(liabilityItem.monthlyAmount());
			liabilityRepository.save(liability);
		}

		PublicCustomerMissedPayment missedPayment = missedPaymentRepository
			.findByFinancialRecord_RecordId(recordId)
			.orElseGet(() -> {
				PublicCustomerMissedPayment entity = new PublicCustomerMissedPayment();
				entity.setFinancialRecord(currentRecord);
				return entity;
			});
		missedPayment.setMissedPayments(request.missedPayments());
		missedPaymentRepository.save(missedPayment);

		touchRecord(currentRecord);
		return new PublicCustomerFinancialStepResponse(
			recordId,
			publicCustomerId,
			"LIABILITIES",
			"Liability and missed-payment step saved successfully."
		);
	}

	@Transactional(readOnly = true)
	public PublicCustomerFinancialRecordResponse getCurrentFinancialRecord(Long publicCustomerId) {
		PublicCustomerFinancialRecord currentRecord = financialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.orElseThrow(() -> new IllegalArgumentException("No current financial record found for this public customer."));

		return mapRecordToResponse(currentRecord);
	}

	@Transactional(readOnly = true)
	public List<PublicCustomerFinancialRecordSummaryResponse> getFinancialRecordHistory(Long publicCustomerId) {
		if (!publicCustomerProfileRepository.existsById(publicCustomerId)) {
			throw new IllegalArgumentException("Public customer not found.");
		}

		return financialRecordRepository
			.findAllByPublicCustomer_PublicCustomerIdOrderByCreatedAtDesc(publicCustomerId)
			.stream()
			.map(financialRecordMapper::toSummary)
			.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public PublicCustomerFinancialRecordResponse getFinancialRecordById(Long publicCustomerId, Long recordId) {
		PublicCustomerFinancialRecord record = financialRecordRepository
			.findByRecordIdAndPublicCustomer_PublicCustomerId(recordId, publicCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Financial record not found for this public customer."));

		return mapRecordToResponse(record);
	}

	private PublicCustomerFinancialRecordResponse mapRecordToResponse(PublicCustomerFinancialRecord record) {
		Long recordId = record.getRecordId();
		int missedPayments = missedPaymentRepository.findByFinancialRecord_RecordId(recordId)
			.map(PublicCustomerMissedPayment::getMissedPayments)
			.orElse(0);

		return financialRecordMapper.toResponse(
			record,
			incomeRepository.findAllByFinancialRecord_RecordId(recordId),
			loanRepository.findAllByFinancialRecord_RecordId(recordId),
			cardRepository.findAllByFinancialRecord_RecordId(recordId),
			liabilityRepository.findAllByFinancialRecord_RecordId(recordId),
			missedPayments
		);
	}

	private String normalizeIncomeCategory(String incomeCategory) {
		String normalized = incomeCategory == null ? "" : incomeCategory.trim().toUpperCase(Locale.ROOT);
		if ("SALARY WORKER".equals(normalized)) {
			return "SALARY";
		}
		if ("BUSINESS PERSON".equals(normalized)) {
			return "BUSINESS";
		}
		if (!"SALARY".equals(normalized) && !"BUSINESS".equals(normalized)) {
			throw new IllegalArgumentException("Income category must be SALARY or BUSINESS.");
		}
		return normalized;
	}

	private PublicCustomerFinancialRecord getOrCreateCurrentRecord(Long publicCustomerId) {
		PublicCustomerProfile profile = publicCustomerProfileRepository.findById(publicCustomerId)
			.orElseThrow(() -> new IllegalArgumentException("Public customer not found."));

		return financialRecordRepository
			.findByPublicCustomer_PublicCustomerIdAndRecordStatus(publicCustomerId, "CURRENT")
			.orElseGet(() -> {
				PublicCustomerFinancialRecord record = new PublicCustomerFinancialRecord();
				record.setPublicCustomer(profile);
				record.setRecordStatus("CURRENT");
				return financialRecordRepository.save(record);
			});
	}

	private void touchRecord(PublicCustomerFinancialRecord record) {
		record.setUpdatedAt(LocalDateTime.now());
		financialRecordRepository.save(record);
	}
}
