package com.bank_web_app.backend.creditlens.service;

import static com.bank_web_app.backend.creditlens.service.CreditEvaluationAmounts.safeAmount;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationAmounts.toPercentage;
import static com.bank_web_app.backend.creditlens.service.CreditEvaluationText.toTitleCase;

import com.bank_web_app.backend.creditlens.dto.response.CreditDashboardFactorResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditDashboardResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInfoTooltipResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInsightItemResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditInsightsResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditReportResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditReportSnapshotResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditRiskFactorResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendPointResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendResponse;
import com.bank_web_app.backend.creditlens.dto.response.CreditTrendSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CreditEvaluationResponseService {

	private static final int LOW_RISK_MAX_POINTS = 33;
	private static final int MEDIUM_RISK_MAX_POINTS = 66;
	private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH);
	private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH);
	private static final DateTimeFormatter SHORT_MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
	private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu, hh:mm a", Locale.ENGLISH);

	private final CreditEvaluationRecordService creditEvaluationRecordService;

	public CreditEvaluationResponseService(CreditEvaluationRecordService creditEvaluationRecordService) {
		this.creditEvaluationRecordService = creditEvaluationRecordService;
	}

	CreditDashboardResponse buildDashboardResponse(EvaluationView current, List<EvaluationView> history) {
		return new CreditDashboardResponse(
			current.evaluationId(),
			current.totalRiskPoints(),
			current.riskLevel(),
			toRiskDisplayLabel(current.riskLevel()),
			current.createdAt(),
			buildDashboardFactors(current),
			buildTrendResponse(history, "6m"),
			"Decrease your Credit Risk Score",
			"Understand the key factors increasing your credit risk and follow practical steps to improve them.",
			"Learn More"
		);
	}

	CreditTrendResponse buildTrendResponse(List<EvaluationView> history, String rangeKey) {
		String normalizedRange = normalizeTrendRange(rangeKey);
		int monthLimit = "12m".equals(normalizedRange) ? 12 : 6;
		List<EvaluationView> monthlyViews = getLatestEvaluationsPerMonth(history);
		if (monthlyViews.size() > monthLimit) {
			monthlyViews = monthlyViews.subList(monthlyViews.size() - monthLimit, monthlyViews.size());
		}

		DateTimeFormatter labelFormatter = monthLimit == 6 ? DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH) : SHORT_MONTH_LABEL_FORMATTER;
		List<CreditTrendPointResponse> points = monthlyViews.stream()
			.map(view -> new CreditTrendPointResponse(
				YearMonth.from(view.createdAt()).toString(),
				view.createdAt().format(labelFormatter),
				view.totalRiskPoints(),
				view.createdAt()
			))
			.toList();

		return new CreditTrendResponse(
			normalizedRange,
			monthLimit == 6 ? "6 Month View" : "12 Month View",
			points.stream().map(CreditTrendPointResponse::monthLabel).toList(),
			points.stream().map(CreditTrendPointResponse::score).toList(),
			points,
			buildTrendSummary(monthlyViews, normalizedRange)
		);
	}

	CreditInsightsResponse buildInsightsResponse(
		EvaluationView current,
		List<EvaluationView> history,
		RecordBreakdown breakdown
	) {
		List<EvaluationView> monthlyViews = getLatestEvaluationsPerMonth(history);
		List<CreditInsightItemResponse> keyRiskFactors = buildKeyRiskFactors(current);
		List<CreditInsightItemResponse> positiveBehaviors = buildPositiveBehaviors(current, monthlyViews);
		List<CreditInsightItemResponse> financialTips = buildFinancialTips(current, breakdown);

		return new CreditInsightsResponse(
			keyRiskFactors,
			positiveBehaviors,
			financialTips,
			"Get a full credit report",
			"Get a complete summary of your credit profile, risk factors, and recommended next actions.",
			"View Report"
		);
	}

	CreditReportResponse buildReportResponse(
		String customerType,
		String evaluationType,
		List<EvaluationView> history
	) {
		List<EvaluationView> monthlyViews = getLatestEvaluationsPerMonth(history);
		List<CreditReportSnapshotResponse> snapshots = new ArrayList<>();
		for (EvaluationView view : monthlyViews) {
			RecordBreakdown breakdown = creditEvaluationRecordService.loadRecordBreakdown(view);
			snapshots.add(new CreditReportSnapshotResponse(
				view.evaluationId(),
				view.createdAt().format(MONTH_LABEL_FORMATTER),
				breakdown.income(),
				breakdown.loanEmi(),
				breakdown.creditCardBalance(),
				breakdown.creditCardLimit(),
				breakdown.otherLiabilities(),
				view.totalRiskPoints(),
				toRiskDisplayLabel(view.riskLevel()),
				view.evaluationType(),
				view.createdAt(),
				view.createdAt().format(REPORT_DATE_FORMATTER).toUpperCase(Locale.ROOT),
				view.missedPaymentsCount(),
				view.activeFacilitiesCount(),
				toPercentage(view.dtiRatio()),
				toPercentage(view.creditUtilizationRatio()),
				resolveDtiBand(view.dtiRatio()),
				buildRiskFactors(view)
			));
		}

		return new CreditReportResponse(
			customerType,
			evaluationType,
			LocalDateTime.now(),
			snapshots
		);
	}

	String normalizeTrendRange(String range) {
		if (range == null || range.isBlank()) {
			return "6m";
		}
		String normalized = range.trim().toLowerCase(Locale.ROOT);
		if (!"6m".equals(normalized) && !"12m".equals(normalized)) {
			throw new IllegalArgumentException("Trend range must be 6m or 12m.");
		}
		return normalized;
	}

	String formatMonthLabel(EvaluationView view) {
		return view.createdAt().format(MONTH_LABEL_FORMATTER);
	}

	String formatExportTimestamp(LocalDateTime value) {
		return value.format(EXPORT_TIMESTAMP_FORMATTER);
	}

	String toRiskDisplayLabel(String riskLevel) {
		return toTitleCase(riskLevel);
	}

	BigDecimal toPercentageValue(BigDecimal ratio) {
		return toPercentage(ratio);
	}

	String resolveDtiBand(BigDecimal dtiRatio) {
		if (safeAmount(dtiRatio).compareTo(new BigDecimal("0.30")) <= 0) {
			return "Low";
		}
		if (safeAmount(dtiRatio).compareTo(new BigDecimal("0.50")) <= 0) {
			return "Medium";
		}
		return "High";
	}

	List<CreditRiskFactorResponse> buildRiskFactors(EvaluationView view) {
		return List.of(
			new CreditRiskFactorResponse("Payment History", view.paymentHistoryPoints(), 30),
			new CreditRiskFactorResponse("Debt-to-Income", view.dtiPoints(), 25),
			new CreditRiskFactorResponse("Utilization", view.utilizationPoints(), 20),
			new CreditRiskFactorResponse("Income Stability", view.incomeStabilityPoints(), 15),
			new CreditRiskFactorResponse("Active Facilities", view.exposurePoints(), 10)
		);
	}

	private CreditTrendSummaryResponse buildTrendSummary(List<EvaluationView> monthlyViews, String rangeKey) {
		if (monthlyViews.isEmpty()) {
			return new CreditTrendSummaryResponse(
				"No Evaluation Yet",
				0,
				"Trend will appear after the first evaluation is generated.",
				"Generate the first evaluation to identify key drivers.",
				"Need at least one monthly evaluation",
				"Generate your first evaluation",
				"STABLE"
			);
		}

		EvaluationView latest = monthlyViews.get(monthlyViews.size() - 1);
		if (monthlyViews.size() < 2) {
			return new CreditTrendSummaryResponse(
				toRiskSummaryLabel(latest.riskLevel()),
				0,
				"Trend will appear after another monthly evaluation.",
				buildCurrentPrimaryDriver(latest),
				"Need at least two monthly evaluations",
				resolveNextTarget(latest.totalRiskPoints()),
				"STABLE"
			);
		}

		EvaluationView earliest = monthlyViews.get(0);
		int delta = latest.totalRiskPoints() - earliest.totalRiskPoints();
		String direction = delta < 0 ? "IMPROVING" : (delta > 0 ? "WORSENING" : "STABLE");
		String trendText;
		if (delta < 0) {
			trendText = monthlyViews.size() >= ("12m".equals(rangeKey) ? 12 : 6)
				? ("Improved over last " + ("12m".equals(rangeKey) ? "12" : "6") + " months")
				: ("Improved since " + earliest.createdAt().format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)));
		} else if (delta > 0) {
			trendText = monthlyViews.size() >= ("12m".equals(rangeKey) ? 12 : 6)
				? ("Risk increased over last " + ("12m".equals(rangeKey) ? "12" : "6") + " months")
				: ("Risk increased since " + earliest.createdAt().format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)));
		} else {
			trendText = "Score remained stable across recent evaluations";
		}

		BigDecimal averageMonthlyChange = BigDecimal.valueOf(Math.abs(delta))
			.divide(BigDecimal.valueOf(Math.max(1, monthlyViews.size() - 1)), 1, RoundingMode.HALF_UP);
		String momentumText = delta == 0
			? "Average monthly movement is currently flat"
			: (
				(delta < 0 ? "Avg decrease: " : "Avg increase: ") +
				averageMonthlyChange.stripTrailingZeros().toPlainString() +
				" risk pts/month"
			);

		return new CreditTrendSummaryResponse(
			toRiskSummaryLabel(latest.riskLevel()),
			delta,
			trendText,
			resolveBiggestDriver(earliest, latest, direction),
			momentumText,
			resolveNextTarget(latest.totalRiskPoints()),
			direction
		);
	}

	private List<CreditDashboardFactorResponse> buildDashboardFactors(EvaluationView current) {
		return List.of(
			new CreditDashboardFactorResponse("Payment history", current.paymentHistoryPoints(), 30, resolveFactorColor(current.paymentHistoryPoints(), 30), null),
			new CreditDashboardFactorResponse(
				"DTI",
				current.dtiPoints(),
				25,
				resolveFactorColor(current.dtiPoints(), 25),
				new CreditInfoTooltipResponse(
					"Debt-to-Income (DTI)",
					"Shows how much of your monthly income goes toward debt payments.",
					"DTI = (Total monthly debt payments / Gross monthly income) x 100"
				)
			),
			new CreditDashboardFactorResponse(
				"Credit utilization",
				current.utilizationPoints(),
				20,
				resolveFactorColor(current.utilizationPoints(), 20),
				new CreditInfoTooltipResponse(
					"Credit Utilization",
					"Shows how much of your available revolving credit you are currently using.",
					"Utilization = (Total card balances / Total credit limits) x 100"
				)
			),
			new CreditDashboardFactorResponse("Income stability", current.incomeStabilityPoints(), 15, resolveFactorColor(current.incomeStabilityPoints(), 15), null),
			new CreditDashboardFactorResponse("Active Facilities", current.exposurePoints(), 10, resolveFactorColor(current.exposurePoints(), 10), null)
		);
	}

	private List<CreditInsightItemResponse> buildKeyRiskFactors(EvaluationView current) {
		List<FactorSnapshot> factors = List.of(
			new FactorSnapshot(
				"Payment History",
				current.paymentHistoryPoints(),
				30,
				"circle-alert",
				current.missedPaymentsCount() + " missed payment(s) in the last 12 months",
				current.paymentHistoryPoints() + "/30 points",
				null
			),
			new FactorSnapshot(
				"Debt-to-Income",
				current.dtiPoints(),
				25,
				"trending-down",
				"Current: " + formatPercentageLabel(current.dtiRatio()) + " (" + current.dtiPoints() + "/25 points)",
				"Keeping DTI at or below 30% removes DTI risk points.",
				new CreditInfoTooltipResponse(
					"Debt-to-Income (DTI)",
					"Shows how much of your monthly income goes toward debt payments.",
					"DTI = (Total monthly debt payments / Gross monthly income) x 100"
				)
			),
			new FactorSnapshot(
				"Credit Utilization",
				current.utilizationPoints(),
				20,
				"credit-card",
				"Current: " + formatPercentageLabel(current.creditUtilizationRatio()) + " (" + current.utilizationPoints() + "/20 points)",
				"Keeping utilization at or below 40% removes utilization risk points.",
				new CreditInfoTooltipResponse(
					"Credit Utilization",
					"Shows how much of your available revolving credit you are currently using.",
					"Utilization = (Total card balances / Total credit limits) x 100"
				)
			),
			new FactorSnapshot(
				"Income Stability",
				current.incomeStabilityPoints(),
				15,
				"briefcase",
				"Income stability currently contributes " + current.incomeStabilityPoints() + "/15 points.",
				"Stable permanent income or lower business fluctuation helps reduce this factor.",
				null
			),
			new FactorSnapshot(
				"Active Facilities",
				current.exposurePoints(),
				10,
				"building-2",
				"Current: " + current.activeFacilitiesCount() + " active facilities (" + current.exposurePoints() + "/10 points)",
				"Keeping facilities at two or fewer removes exposure points.",
				null
			)
		);

		return factors.stream()
			.sorted(Comparator.comparingInt(FactorSnapshot::points).reversed().thenComparing(FactorSnapshot::title))
			.limit(3)
			.map(factor -> new CreditInsightItemResponse(
				factor.title(),
				factor.description(),
				factor.detail(),
				resolveBadgeText(factor.points(), factor.maxPoints()),
				resolveBadgeTone(factor.points(), factor.maxPoints()),
				factor.iconKey(),
				factor.infoTooltip()
			))
			.toList();
	}

	private List<CreditInsightItemResponse> buildPositiveBehaviors(EvaluationView current, List<EvaluationView> monthlyViews) {
		EvaluationView previous = monthlyViews.size() >= 2 ? monthlyViews.get(monthlyViews.size() - 2) : null;
		List<InsightCandidate> candidates = new ArrayList<>();

		int paymentStrength = current.missedPaymentsCount() == 0 ? 95 : (current.missedPaymentsCount() == 1 ? 75 : (current.missedPaymentsCount() <= 3 ? 50 : 0));
		if (paymentStrength > 0) {
			candidates.add(new InsightCandidate(
				paymentStrength,
				new CreditInsightItemResponse(
					current.missedPaymentsCount() == 0 ? "No recent missed payments" : "Payment history is below the maximum-risk band",
					current.missedPaymentsCount() == 0
						? "No missed payments are currently affecting the last 12-month history."
						: current.missedPaymentsCount() + " missed payment(s) keep this factor below the 4+ highest-risk bracket.",
					"Keeping the next 12 months clean can reduce payment-history risk points further.",
					current.missedPaymentsCount() == 0 ? "CLEAN" : "RECOVERABLE",
					current.missedPaymentsCount() == 0 ? "green" : "amber",
					"check-circle",
					null
				)
			));
		}

		int dtiStrength = current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? 90 : (current.dtiRatio().compareTo(new BigDecimal("0.50")) <= 0 ? 55 : 0);
		if (dtiStrength > 0) {
			candidates.add(new InsightCandidate(
				dtiStrength,
				new CreditInsightItemResponse(
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? "Debt-to-income is within the low-risk band" : "DTI is below the critical threshold",
					"Current DTI is " + formatPercentageLabel(current.dtiRatio()) + ".",
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0
						? "This keeps DTI at 0/25 points."
						: "Keeping DTI at or below 50% avoids the highest DTI penalty.",
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? "STRONG" : "CONTROLLED",
					current.dtiRatio().compareTo(new BigDecimal("0.30")) <= 0 ? "green" : "amber",
					"percent",
					null
				)
			));
		}

		int utilizationStrength = current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0
			? 85
			: (current.creditUtilizationRatio().compareTo(new BigDecimal("0.70")) <= 0 ? 55 : 0);
		if (utilizationStrength > 0) {
			candidates.add(new InsightCandidate(
				utilizationStrength,
				new CreditInsightItemResponse(
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0
						? "Credit utilization is within the healthy band"
						: "Utilization is below the maximum-risk zone",
					"Current utilization is " + formatPercentageLabel(current.creditUtilizationRatio()) + ".",
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0
						? "This keeps utilization at 0/20 points."
						: "Staying at or below 70% avoids the maximum utilization penalty.",
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0 ? "HEALTHY" : "CONTROLLED",
					current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) <= 0 ? "green" : "amber",
					"credit-card",
					null
				)
			));
		}

		int exposureStrength = current.activeFacilitiesCount() <= 2 ? 80 : (current.activeFacilitiesCount() <= 4 ? 55 : 0);
		if (exposureStrength > 0) {
			candidates.add(new InsightCandidate(
				exposureStrength,
				new CreditInsightItemResponse(
					current.activeFacilitiesCount() <= 2 ? "Credit exposure is well controlled" : "Active facilities remain manageable",
					"Current exposure is " + current.activeFacilitiesCount() + " active facilities.",
					current.activeFacilitiesCount() <= 2
						? "This keeps exposure at 0/10 points."
						: "Keeping facilities at four or fewer avoids the highest exposure penalty.",
					current.activeFacilitiesCount() <= 2 ? "BALANCED" : "MANAGEABLE",
					current.activeFacilitiesCount() <= 2 ? "green" : "amber",
					"building-2",
					null
				)
			));
		}

		int incomeStrength = current.incomeStabilityPoints() == 0 ? 82 : (current.incomeStabilityPoints() <= 7 ? 50 : 0);
		if (incomeStrength > 0) {
			candidates.add(new InsightCandidate(
				incomeStrength,
				new CreditInsightItemResponse(
					current.incomeStabilityPoints() == 0 ? "Income profile is stable" : "Income stability is partially supportive",
					"Income stability currently contributes " + current.incomeStabilityPoints() + "/15 points.",
					current.incomeStabilityPoints() == 0
						? "Stable employment or business patterns are helping the score."
						: "Income stability is not in the highest-risk tier.",
					current.incomeStabilityPoints() == 0 ? "VERIFIED" : "SUPPORTIVE",
					current.incomeStabilityPoints() == 0 ? "green" : "blue",
					"badge-check",
					null
				)
			));
		}

		if (previous != null) {
			if (current.totalRiskPoints() < previous.totalRiskPoints()) {
				candidates.add(new InsightCandidate(
					78,
					new CreditInsightItemResponse(
						"Risk score improved since the last evaluation",
						"Score moved from " + previous.totalRiskPoints() + " to " + current.totalRiskPoints() + ".",
						"Recent behavior is moving the profile in the right direction.",
						"IMPROVING",
						"green",
						"trending-up",
						null
					)
				));
			} else if (current.totalRiskPoints().equals(previous.totalRiskPoints())) {
				candidates.add(new InsightCandidate(
					35,
					new CreditInsightItemResponse(
						"Risk score remained stable",
						"Score stayed at " + current.totalRiskPoints() + " since the last evaluation.",
						"Maintaining stability helps prevent a move into a worse risk band.",
						"STABLE",
						"blue",
						"activity",
						null
					)
				));
			}
		}

		return candidates.stream()
			.sorted(Comparator.comparingInt(InsightCandidate::priority).reversed())
			.limit(3)
			.map(InsightCandidate::item)
			.toList();
	}

	private List<CreditInsightItemResponse> buildFinancialTips(EvaluationView current, RecordBreakdown breakdown) {
		List<InsightCandidate> candidates = new ArrayList<>();

		if (current.creditUtilizationRatio().compareTo(new BigDecimal("0.70")) > 0) {
			candidates.add(new InsightCandidate(
				95,
				new CreditInsightItemResponse(
					"Bring utilization below 70% first",
					"Current utilization is " + formatPercentageLabel(current.creditUtilizationRatio()) + ", which is in the highest-risk band.",
					"Below 70% can drop utilization risk by 10 points. Below 40% can remove up to " + current.utilizationPoints() + " points.",
					"-10 TO -" + current.utilizationPoints() + " PTS",
					"amber",
					"credit-card",
					null
				)
			));
		} else if (current.creditUtilizationRatio().compareTo(new BigDecimal("0.40")) > 0) {
			candidates.add(new InsightCandidate(
				85,
				new CreditInsightItemResponse(
					"Bring utilization below 40%",
					"Current utilization is " + formatPercentageLabel(current.creditUtilizationRatio()) + ".",
					"Reducing balances before the statement date can remove the current " + current.utilizationPoints() + " utilization points.",
					"-" + current.utilizationPoints() + " PTS",
					"amber",
					"credit-card",
					null
				)
			));
		}

		if (current.dtiRatio().compareTo(new BigDecimal("0.50")) > 0) {
			candidates.add(new InsightCandidate(
				92,
				new CreditInsightItemResponse(
					"Reduce DTI below 50% first",
					"Current DTI is " + formatPercentageLabel(current.dtiRatio()) + ".",
					"Below 50% reduces DTI points from 25 to 12. Below 30% removes the factor entirely.",
					"-13 TO -" + current.dtiPoints() + " PTS",
					"amber",
					"percent",
					null
				)
			));
		} else if (current.dtiRatio().compareTo(new BigDecimal("0.30")) > 0) {
			candidates.add(new InsightCandidate(
				80,
				new CreditInsightItemResponse(
					"Reduce DTI below 30%",
					"Current DTI is " + formatPercentageLabel(current.dtiRatio()) + ".",
					"Paying down monthly debt or increasing stable income can remove the current " + current.dtiPoints() + " DTI points.",
					"-" + current.dtiPoints() + " PTS",
					"amber",
					"percent",
					null
				)
			));
		}

		if (current.paymentHistoryPoints() > 0) {
			candidates.add(new InsightCandidate(
				88,
				new CreditInsightItemResponse(
					"Avoid any new missed payments",
					"Payment history currently contributes " + current.paymentHistoryPoints() + "/30 points.",
					"Autopay, due-date reminders, and clearing overdue amounts help future evaluations remove these points over time.",
					"UP TO -" + current.paymentHistoryPoints() + " PTS",
					"amber",
					"circle-alert",
					null
				)
			));
		}

		if (current.exposurePoints() > 0) {
			int immediateGain = current.activeFacilitiesCount() >= 5 ? 5 : current.exposurePoints();
			candidates.add(new InsightCandidate(
				65,
				new CreditInsightItemResponse(
					"Limit new facilities and reduce active exposure",
					"Current exposure is " + current.activeFacilitiesCount() + " active facilities.",
					"Closing unused cards or avoiding new borrowing can reduce exposure points by " + immediateGain + " or more.",
					"UP TO -" + current.exposurePoints() + " PTS",
					"blue",
					"building-2",
					null
				)
			));
		}

		if (current.incomeStabilityPoints() > 0) {
			candidates.add(new InsightCandidate(
				55,
				new CreditInsightItemResponse(
					"Strengthen proof of stable income",
					"Income stability currently contributes " + current.incomeStabilityPoints() + "/15 points.",
					"Longer employment continuity, stable contracts, or lower business fluctuation can reduce this factor.",
					"UP TO -" + current.incomeStabilityPoints() + " PTS",
					"blue",
					"briefcase",
					null
				)
			));
		}

		return candidates.stream()
			.sorted(Comparator.comparingInt(InsightCandidate::priority).reversed())
			.limit(3)
			.map(InsightCandidate::item)
			.toList();
	}

	private List<EvaluationView> getLatestEvaluationsPerMonth(List<EvaluationView> history) {
		Map<YearMonth, EvaluationView> latestByMonth = new LinkedHashMap<>();
		history.stream()
			.sorted(Comparator.comparing(EvaluationView::createdAt))
			.forEach(view -> latestByMonth.put(YearMonth.from(view.createdAt()), view));
		return new ArrayList<>(latestByMonth.values());
	}

	private String resolveBiggestDriver(EvaluationView earliest, EvaluationView latest, String direction) {
		Map<String, Integer> deltas = Map.of(
			"PAYMENT", latest.paymentHistoryPoints() - earliest.paymentHistoryPoints(),
			"DTI", latest.dtiPoints() - earliest.dtiPoints(),
			"UTILIZATION", latest.utilizationPoints() - earliest.utilizationPoints(),
			"INCOME", latest.incomeStabilityPoints() - earliest.incomeStabilityPoints(),
			"EXPOSURE", latest.exposurePoints() - earliest.exposurePoints()
		);

		if ("IMPROVING".equals(direction)) {
			Map.Entry<String, Integer> best = deltas.entrySet()
				.stream()
				.min(Map.Entry.comparingByValue())
				.orElse(null);
			if (best != null && best.getValue() < 0) {
				return switch (best.getKey()) {
					case "PAYMENT" -> "Fewer missed-payment points";
					case "DTI" -> "Reduced DTI pressure";
					case "UTILIZATION" -> "Lower utilization over time";
					case "INCOME" -> "More stable income profile";
					case "EXPOSURE" -> "Lower active credit exposure";
					default -> "Improved factor mix";
				};
			}
		}

		if ("WORSENING".equals(direction)) {
			Map.Entry<String, Integer> worst = deltas.entrySet()
				.stream()
				.max(Map.Entry.comparingByValue())
				.orElse(null);
			if (worst != null && worst.getValue() > 0) {
				return switch (worst.getKey()) {
					case "PAYMENT" -> "Payment history deterioration";
					case "DTI" -> "Higher DTI pressure";
					case "UTILIZATION" -> "Higher credit utilization";
					case "INCOME" -> "Less stable income profile";
					case "EXPOSURE" -> "Higher active credit exposure";
					default -> "Worsening factor mix";
				};
			}
		}

		return buildCurrentPrimaryDriver(latest);
	}

	private String buildCurrentPrimaryDriver(EvaluationView current) {
		Map<String, Integer> points = Map.of(
			"Payment history", current.paymentHistoryPoints(),
			"Debt-to-income pressure", current.dtiPoints(),
			"Credit utilization", current.utilizationPoints(),
			"Income stability", current.incomeStabilityPoints(),
			"Active facilities", current.exposurePoints()
		);
		return points.entrySet()
			.stream()
			.max(Map.Entry.comparingByValue())
			.map(entry -> entry.getValue() <= 0 ? "Risk profile is currently well balanced" : (entry.getKey() + " remains the biggest pressure"))
			.orElse("Risk profile is currently well balanced");
	}

	private String resolveNextTarget(int score) {
		if (score > MEDIUM_RISK_MAX_POINTS) {
			return "Reduce " + (score - MEDIUM_RISK_MAX_POINTS) + " risk pts to reach Medium Risk";
		}
		if (score > LOW_RISK_MAX_POINTS) {
			return "Reduce " + (score - LOW_RISK_MAX_POINTS) + " risk pts to reach Low Risk";
		}
		return "Stay at 33 or below to remain in Low Risk";
	}

	private String resolveBadgeText(int value, int max) {
		if (value <= 0) {
			return "LOW";
		}
		BigDecimal ratio = BigDecimal.valueOf(value)
			.divide(BigDecimal.valueOf(Math.max(1, max)), 4, RoundingMode.HALF_UP);
		if (ratio.compareTo(BigDecimal.ONE) >= 0) {
			return "MAX RISK";
		}
		if (ratio.compareTo(new BigDecimal("0.66")) >= 0) {
			return "HIGH";
		}
		if (ratio.compareTo(new BigDecimal("0.33")) >= 0) {
			return "MEDIUM";
		}
		return "LOW";
	}

	private String resolveBadgeTone(int value, int max) {
		String badgeText = resolveBadgeText(value, max);
		return switch (badgeText) {
			case "MAX RISK" -> "red";
			case "HIGH" -> "orange";
			case "MEDIUM" -> "amber";
			default -> "green";
		};
	}

	private String resolveFactorColor(int value, int max) {
		BigDecimal ratio = BigDecimal.valueOf(value)
			.divide(BigDecimal.valueOf(Math.max(1, max)), 4, RoundingMode.HALF_UP);
		if (ratio.compareTo(new BigDecimal("0.33")) <= 0) {
			return "#34d399";
		}
		if (ratio.compareTo(new BigDecimal("0.66")) <= 0) {
			return "#fbbf24";
		}
		return "#ef4444";
	}

	private String formatPercentageLabel(BigDecimal ratio) {
		return toPercentage(ratio).stripTrailingZeros().toPlainString() + "%";
	}

	private String toRiskSummaryLabel(String riskLevel) {
		return toTitleCase(riskLevel) + " Risk";
	}
}
