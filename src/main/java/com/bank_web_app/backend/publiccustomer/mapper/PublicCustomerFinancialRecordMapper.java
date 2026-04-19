package com.bank_web_app.backend.publiccustomer.mapper;

import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordResponse;
import com.bank_web_app.backend.publiccustomer.dto.response.PublicCustomerFinancialRecordSummaryResponse;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerCard;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerFinancialRecord;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerIncome;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLiability;
import com.bank_web_app.backend.publiccustomer.entity.PublicCustomerLoan;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PublicCustomerFinancialRecordMapper {

	public PublicCustomerFinancialRecordSummaryResponse toSummary(PublicCustomerFinancialRecord record) {
		return new PublicCustomerFinancialRecordSummaryResponse(
			record.getRecordId(),
			record.getPublicCustomer().getPublicCustomerId(),
			record.getRecordStatus(),
			record.getCreatedAt(),
			record.getUpdatedAt()
		);
	}

	public PublicCustomerFinancialRecordResponse toResponse(
		PublicCustomerFinancialRecord record,
		List<PublicCustomerIncome> incomes,
		List<PublicCustomerLoan> loans,
		List<PublicCustomerCard> cards,
		List<PublicCustomerLiability> liabilities,
		int missedPayments
	) {
		List<PublicCustomerFinancialRecordResponse.IncomeItem> incomeItems = incomes
			.stream()
			.sorted(Comparator.comparing(PublicCustomerIncome::getIncomeId))
			.map(income -> new PublicCustomerFinancialRecordResponse.IncomeItem(
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

		List<PublicCustomerFinancialRecordResponse.LoanItem> loanItems = loans
			.stream()
			.sorted(Comparator.comparing(PublicCustomerLoan::getLoanId))
			.map(loan -> new PublicCustomerFinancialRecordResponse.LoanItem(
				loan.getLoanId(),
				loan.getLoanType(),
				loan.getMonthlyEmi(),
				loan.getRemainingBalance(),
				loan.getCreatedAt()
			))
			.collect(Collectors.toList());

		List<PublicCustomerFinancialRecordResponse.CardItem> cardItems = cards
			.stream()
			.sorted(Comparator.comparing(PublicCustomerCard::getCardId))
			.map(card -> new PublicCustomerFinancialRecordResponse.CardItem(
				card.getCardId(),
				card.getProvider(),
				card.getCreditLimit(),
				card.getOutstandingBalance(),
				card.getCreatedAt()
			))
			.collect(Collectors.toList());

		List<PublicCustomerFinancialRecordResponse.LiabilityItem> liabilityItems = liabilities
			.stream()
			.sorted(Comparator.comparing(PublicCustomerLiability::getLiabilityId))
			.map(liability -> new PublicCustomerFinancialRecordResponse.LiabilityItem(
				liability.getLiabilityId(),
				liability.getDescription(),
				liability.getMonthlyAmount(),
				liability.getCreatedAt()
			))
			.collect(Collectors.toList());

		return new PublicCustomerFinancialRecordResponse(
			record.getRecordId(),
			record.getPublicCustomer().getPublicCustomerId(),
			record.getRecordStatus(),
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
