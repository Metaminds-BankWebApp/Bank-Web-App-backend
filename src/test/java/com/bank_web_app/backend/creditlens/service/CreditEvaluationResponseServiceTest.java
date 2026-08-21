package com.bank_web_app.backend.creditlens.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank_web_app.backend.creditlens.dto.response.CreditInsightsResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditReportResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditEvaluationResponseServiceTest {

	@Mock
	private CreditEvaluationRecordService recordService;

	@Test
	void reportBulkLoadsOnlyTheLatestEvaluationFromEachMonth() {
		CreditEvaluationResponseService responseService = new CreditEvaluationResponseService(recordService);
		EvaluationView earlyJanuary = view(1L, 11L, LocalDateTime.of(2026, 1, 1, 9, 0));
		EvaluationView lateJanuary = view(2L, 12L, LocalDateTime.of(2026, 1, 20, 9, 0));
		EvaluationView february = view(3L, 13L, LocalDateTime.of(2026, 2, 1, 9, 0));
		RecordBreakdown januaryBreakdown = breakdown("125000.00");
		RecordBreakdown februaryBreakdown = breakdown("150000.00");
		when(recordService.loadRecordBreakdowns(anyList())).thenReturn(Map.of(
			12L,
			januaryBreakdown,
			13L,
			februaryBreakdown
		));

		CreditReportResponse response = responseService.buildReportResponse(
			"PUBLIC_CUSTOMER",
			"Self Assessment",
			List.of(february, earlyJanuary, lateJanuary)
		);

		assertThat(response.snapshots()).extracting(snapshot -> snapshot.evaluationId()).containsExactly(2L, 3L);
		assertThat(response.snapshots()).extracting(snapshot -> snapshot.income()).containsExactly(
			new BigDecimal("125000.00"),
			new BigDecimal("150000.00")
		);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<EvaluationView>> viewsCaptor = ArgumentCaptor.forClass(List.class);
		verify(recordService).loadRecordBreakdowns(viewsCaptor.capture());
		assertThat(viewsCaptor.getValue()).extracting(EvaluationView::evaluationId).containsExactly(2L, 3L);
		verify(recordService, never()).loadRecordBreakdown(org.mockito.ArgumentMatchers.any(EvaluationView.class));
	}

	@Test
	void insightsExcludeZeroPointKeyRiskFactors() {
		CreditEvaluationResponseService responseService = new CreditEvaluationResponseService(recordService);
		EvaluationView current = viewWithFactors(10, 12, 0, 0, 0, 1, 1);

		CreditInsightsResponse response = responseService.buildInsightsResponse(
			current,
			List.of(current),
			breakdown("125000.00")
		);

		assertThat(response.keyRiskFactors())
			.extracting(item -> item.title())
			.containsExactly("Debt-to-Income", "Payment History");
	}

	@Test
	void positiveBehaviorsExcludeFullPointFactorsAndUseNextEligiblePriorities() {
		CreditEvaluationResponseService responseService = new CreditEvaluationResponseService(recordService);
		EvaluationView current = viewWithFactors(30, 0, 0, 0, 0, 1, 0);

		CreditInsightsResponse response = responseService.buildInsightsResponse(
			current,
			List.of(current),
			breakdown("125000.00")
		);

		assertThat(response.positiveBehaviors())
			.extracting(item -> item.title())
			.containsExactly(
				"Debt-to-income is within the low-risk band",
				"Credit utilization is within the healthy band",
				"Income profile is stable"
			)
			.doesNotContain("No recent missed payments");
	}

	private RecordBreakdown breakdown(String income) {
		return new RecordBreakdown(
			new BigDecimal(income),
			new BigDecimal("30000.00"),
			new BigDecimal("15000.00"),
			new BigDecimal("100000.00"),
			new BigDecimal("5000.00"),
			new BigDecimal("450.00")
		);
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

	private EvaluationView viewWithFactors(
		int paymentHistoryPoints,
		int dtiPoints,
		int utilizationPoints,
		int incomeStabilityPoints,
		int exposurePoints,
		int activeFacilitiesCount,
		int missedPaymentsCount
	) {
		return new EvaluationView(
			100L,
			200L,
			"PUBLIC",
			"Self Assessment",
			paymentHistoryPoints + dtiPoints + utilizationPoints + incomeStabilityPoints + exposurePoints,
			"LOW",
			new BigDecimal("125000.00"),
			new BigDecimal("25000.00"),
			new BigDecimal("100000.00"),
			new BigDecimal("20000.00"),
			new BigDecimal("0.20"),
			new BigDecimal("0.20"),
			activeFacilitiesCount,
			missedPaymentsCount,
			paymentHistoryPoints,
			dtiPoints,
			utilizationPoints,
			incomeStabilityPoints,
			exposurePoints,
			LocalDateTime.of(2026, 8, 21, 9, 0)
		);
	}
}
