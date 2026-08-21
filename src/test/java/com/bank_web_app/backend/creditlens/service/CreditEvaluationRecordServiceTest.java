package com.bank_web_app.backend.creditlens.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditEvaluationRecordServiceTest {

	@Mock
	private PublicCustomerFinancialRecordRepository publicFinancialRecordRepository;

	@Mock
	private PublicCustomerIncomeRepository publicIncomeRepository;

	@Mock
	private PublicCustomerLoanRepository publicLoanRepository;

	@Mock
	private PublicCustomerCardRepository publicCardRepository;

	@Mock
	private PublicCustomerLiabilityRepository publicLiabilityRepository;

	@Mock
	private BankCustomerFinancialRecordRepository bankFinancialRecordRepository;

	@Mock
	private BankCustomerIncomeRepository bankIncomeRepository;

	@Mock
	private BankCustomerLoanRepository bankLoanRepository;

	@Mock
	private BankCustomerCardRepository bankCardRepository;

	@Mock
	private BankCustomerLiabilityRepository bankLiabilityRepository;

	private CreditEvaluationRecordService recordService;

	@BeforeEach
	void setUp() {
		recordService = new CreditEvaluationRecordService(
			publicFinancialRecordRepository,
			publicIncomeRepository,
			publicLoanRepository,
			publicCardRepository,
			publicLiabilityRepository,
			bankFinancialRecordRepository,
			bankIncomeRepository,
			bankLoanRepository,
			bankCardRepository,
			bankLiabilityRepository
		);
	}

	@Test
	void loadsPublicReportBreakdownsWithOneQueryPerFinancialTable() {
		PublicCustomerFinancialRecord january = financialRecord(11L);
		PublicCustomerFinancialRecord february = financialRecord(12L);
		List<Long> recordIds = List.of(11L, 12L);

		when(publicIncomeRepository.findAllByFinancialRecord_RecordIdIn(recordIds)).thenReturn(List.of(
			income(january, "100000.00"),
			income(january, "25000.00"),
			income(february, "150000.00")
		));
		when(publicLoanRepository.findAllByFinancialRecord_RecordIdIn(recordIds)).thenReturn(List.of(
			loan(january, "30000.00"),
			loan(february, "40000.00")
		));
		when(publicCardRepository.findAllByFinancialRecord_RecordIdIn(recordIds)).thenReturn(List.of(
			card(january, "15000.00", "100000.00"),
			card(february, "20000.00", "120000.00")
		));
		when(publicLiabilityRepository.findAllByFinancialRecord_RecordIdIn(recordIds)).thenReturn(List.of(
			liability(january, "5000.00")
		));

		Map<Long, RecordBreakdown> breakdowns = recordService.loadRecordBreakdowns(List.of(
			view(1L, 11L, LocalDateTime.of(2026, 1, 1, 9, 0)),
			view(2L, 12L, LocalDateTime.of(2026, 2, 1, 9, 0))
		));

		assertThat(breakdowns).hasSize(2);
		assertThat(breakdowns.get(11L).income()).isEqualByComparingTo("125000.00");
		assertThat(breakdowns.get(11L).loanEmi()).isEqualByComparingTo("30000.00");
		assertThat(breakdowns.get(11L).creditCardBalance()).isEqualByComparingTo("15000.00");
		assertThat(breakdowns.get(11L).creditCardLimit()).isEqualByComparingTo("100000.00");
		assertThat(breakdowns.get(11L).otherLiabilities()).isEqualByComparingTo("5000.00");
		assertThat(breakdowns.get(12L).income()).isEqualByComparingTo("150000.00");
		assertThat(breakdowns.get(12L).otherLiabilities()).isEqualByComparingTo("0.00");

		verify(publicIncomeRepository).findAllByFinancialRecord_RecordIdIn(recordIds);
		verify(publicLoanRepository).findAllByFinancialRecord_RecordIdIn(recordIds);
		verify(publicCardRepository).findAllByFinancialRecord_RecordIdIn(recordIds);
		verify(publicLiabilityRepository).findAllByFinancialRecord_RecordIdIn(recordIds);
		verify(publicIncomeRepository, never()).findAllByFinancialRecord_RecordId(anyLong());
		verify(publicLoanRepository, never()).findAllByFinancialRecord_RecordId(anyLong());
		verify(publicCardRepository, never()).findAllByFinancialRecord_RecordId(anyLong());
		verify(publicLiabilityRepository, never()).findAllByFinancialRecord_RecordId(anyLong());
	}

	private PublicCustomerFinancialRecord financialRecord(Long recordId) {
		PublicCustomerFinancialRecord record = new PublicCustomerFinancialRecord();
		record.setRecordId(recordId);
		return record;
	}

	private PublicCustomerIncome income(PublicCustomerFinancialRecord record, String amount) {
		PublicCustomerIncome income = new PublicCustomerIncome();
		income.setFinancialRecord(record);
		income.setAmount(new BigDecimal(amount));
		return income;
	}

	private PublicCustomerLoan loan(PublicCustomerFinancialRecord record, String monthlyEmi) {
		PublicCustomerLoan loan = new PublicCustomerLoan();
		loan.setFinancialRecord(record);
		loan.setMonthlyEmi(new BigDecimal(monthlyEmi));
		return loan;
	}

	private PublicCustomerCard card(PublicCustomerFinancialRecord record, String balance, String limit) {
		PublicCustomerCard card = new PublicCustomerCard();
		card.setFinancialRecord(record);
		card.setOutstandingBalance(new BigDecimal(balance));
		card.setCreditLimit(new BigDecimal(limit));
		return card;
	}

	private PublicCustomerLiability liability(PublicCustomerFinancialRecord record, String amount) {
		PublicCustomerLiability liability = new PublicCustomerLiability();
		liability.setFinancialRecord(record);
		liability.setMonthlyAmount(new BigDecimal(amount));
		return liability;
	}

	private EvaluationView view(Long evaluationId, Long recordId, LocalDateTime createdAt) {
		return new EvaluationView(
			evaluationId,
			recordId,
			"PUBLIC",
			"Self Assessment",
			20,
			"LOW",
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			BigDecimal.ZERO,
			1,
			0,
			0,
			0,
			0,
			0,
			0,
			createdAt
		);
	}
}
