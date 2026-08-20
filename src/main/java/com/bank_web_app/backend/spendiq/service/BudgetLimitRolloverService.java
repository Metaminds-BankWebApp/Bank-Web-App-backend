package com.bank_web_app.backend.spendiq.service;

import com.bank_web_app.backend.spendiq.entity.BudgetLimit;
import com.bank_web_app.backend.spendiq.entity.BudgetLimitSource;
import com.bank_web_app.backend.spendiq.repository.BudgetLimitRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetLimitRolloverService {

	private final BudgetLimitRepository budgetLimitRepository;

	public BudgetLimitRolloverService(BudgetLimitRepository budgetLimitRepository) {
		this.budgetLimitRepository = budgetLimitRepository;
	}

	@Transactional
	public int rolloverBudgetsToNextMonth() {
		YearMonth current = YearMonth.from(LocalDate.now());
		return rolloverBudgets(current, current.plusMonths(1));
	}

	private int rolloverBudgets(YearMonth from, YearMonth to) {
		List<BudgetLimit> sourceBudgets = budgetLimitRepository.findAllByMonthAndYear(from.getMonthValue(), from.getYear());

		int created = 0;
		for (BudgetLimit budget : sourceBudgets) {
			boolean alreadyExists = budgetLimitRepository
				.findByUser_UserIdAndCategory_CategoryIdAndMonthAndYear(
					budget.getUser().getUserId(),
					budget.getCategory().getCategoryId(),
					to.getMonthValue(),
					to.getYear()
				)
				.isPresent();
			if (alreadyExists) continue;

			BudgetLimit rolledOver = new BudgetLimit();
			rolledOver.setUser(budget.getUser());
			rolledOver.setCategory(budget.getCategory());
			rolledOver.setBudgetAmount(budget.getBudgetAmount());
			rolledOver.setMonth(to.getMonthValue());
			rolledOver.setYear(to.getYear());
			rolledOver.setSource(BudgetLimitSource.ROLLOVER);
			budgetLimitRepository.save(rolledOver);
			created++;
		}
		return created;
	}
}
