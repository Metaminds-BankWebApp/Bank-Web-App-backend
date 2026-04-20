package com.bank_web_app.backend.bankcustomer.mapper;

import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordResponse;
import com.bank_web_app.backend.bankcustomer.dto.response.BankCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerCard;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerFinancialRecord;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerIncome;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLiability;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomerLoan;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BankCustomerFinancialRecordMapper {

	public BankCustomerFinancialRecordSummaryResponse toSummary(BankCustomerFinancialRecord record) {
		return new BankCustomerFinancialRecordSummaryResponse(
			record.getBankRecordId(),
			record.getBankCustomer().getBankCustomerId(),
			record.getVerifiedByOfficer().getOfficerId(),
			record.getDataSource(),
			record.getCreatedAt(),
			record.getUpdatedAt()
		);
	}

	public BankCustomerFinancialRecordResponse toResponse(
		BankCustomerFinancialRecord record,
		List<BankCustomerIncome> incomes,
		List<BankCustomerLoan> loans,
		List<BankCustomerCard> cards,
		List<BankCustomerLiability> liabilities,
		int missedPayments
	) {
		List<BankCustomerFinancialRecordResponse.IncomeItem> incomeItems = incomes
			.stream()
			.sorted(Comparator.comparing(BankCustomerIncome::getIncomeId))
			.map(income -> new BankCustomerFinancialRecordResponse.IncomeItem(
				income.getIncomeId(),
				income.getIncomeCategory(),
				income.getAmount(),
				income.getSalaryType(),
				income.getEmploymentType(),
				income.getContractDurationMonths(),
				income.getIncomeStability(),
				income.getCreatedAt()
			))
			.collect(Collectors.toList());

		List<BankCustomerFinancialRecordResponse.LoanItem> loanItems = loans
			.stream()
			.sorted(Comparator.comparing(BankCustomerLoan::getLoanId))
			.map(loan -> new BankCustomerFinancialRecordResponse.LoanItem(
				loan.getLoanId(),
				loan.getLoanType(),
				loan.getMonthlyEmi(),
				loan.getRemainingBalance(),
				loan.getCreatedAt()
			))
			.collect(Collectors.toList());

		List<BankCustomerFinancialRecordResponse.CardItem> cardItems = cards
			.stream()
			.sorted(Comparator.comparing(BankCustomerCard::getCardId))
			.map(card -> new BankCustomerFinancialRecordResponse.CardItem(
				card.getCardId(),
				card.getProvider(),
				card.getCreditLimit(),
				card.getOutstandingBalance(),
				card.getCreatedAt()
			))
			.collect(Collectors.toList());

		List<BankCustomerFinancialRecordResponse.LiabilityItem> liabilityItems = liabilities
			.stream()
			.sorted(Comparator.comparing(BankCustomerLiability::getLiabilityId))
			.map(liability -> new BankCustomerFinancialRecordResponse.LiabilityItem(
				liability.getLiabilityId(),
				liability.getDescription(),
				liability.getMonthlyAmount(),
				liability.getCreatedAt()
			))
			.collect(Collectors.toList());

		return new BankCustomerFinancialRecordResponse(
			record.getBankRecordId(),
			record.getBankCustomer().getBankCustomerId(),
			record.getVerifiedByOfficer().getOfficerId(),
			record.getDataSource(),
			record.getCreatedAt(),
			record.getUpdatedAt(),
			incomeItems,
			loanItems,
			cardItems,
			liabilityItems,
			missedPayments
		);
	}
}
