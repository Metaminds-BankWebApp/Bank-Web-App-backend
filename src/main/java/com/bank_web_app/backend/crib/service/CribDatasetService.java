package com.bank_web_app.backend.crib.service;

import com.bank_web_app.backend.crib.config.CribDatasetProperties;
import com.bank_web_app.backend.crib.dto.response.CribDatasetSnapshotResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CribDatasetService {

	private static final String TABLE_CRIB_CUSTOMERS = "crib_customers";
	private static final String TABLE_CRIB_LOAN_RECORDS = "crib_loan_records";
	private static final String TABLE_CRIB_CARD_RECORDS = "crib_credit_card_records";
	private static final String TABLE_CRIB_OTHER_LIABILITY_RECORDS = "crib_other_liability_records";
	private static final String TABLE_CRIB_PAYMENT_HISTORY = "crib_payment_history";
	private static final String TABLE_CRIB_CREDIT_SUMMARY = "crib_credit_summary";

	private static final Set<String> NIC_COLUMNS = Set.of(
		"nic",
		"national_id",
		"national_identity_card",
		"national_identity_card_number",
		"id_number",
		"identity_number"
	);
	private static final Set<String> LOAN_HINTS = Set.of("loan", "facility", "borrowing", "emi", "installment");
	private static final Set<String> CARD_HINTS = Set.of("card", "credit");
	private static final Set<String> LIABILITY_HINTS = Set.of("liabil", "debt", "obligation", "overdue", "default");
	private static final Set<String> INQUIRY_HINTS = Set.of("inquir", "enquir");
	private final JdbcTemplate cribJdbcTemplate;
	private final CribDatasetProperties properties;

	public CribDatasetService(
		@Qualifier("cribJdbcTemplate") JdbcTemplate cribJdbcTemplate,
		CribDatasetProperties properties
	) {
		this.cribJdbcTemplate = cribJdbcTemplate;
		this.properties = properties;
	}

	public CribDatasetSnapshotResponse lookupSnapshotByNic(String nic) {
		String normalizedNic = normalize(nic);
		if (normalizedNic.isBlank()) {
			throw new IllegalArgumentException("NIC is required to retrieve CRIB data.");
		}
		if (!properties.isConfigured()) {
			throw new IllegalStateException("CRIB datasource is not configured. Set crib.datasource.url, username, and password.");
		}

		return cribJdbcTemplate.execute((ConnectionCallback<CribDatasetSnapshotResponse>) connection -> {
			DatabaseMetaData metaData = connection.getMetaData();
			if (hasTable(metaData, TABLE_CRIB_CUSTOMERS)) {
				return lookupSnapshotUsingCribCustomerSchema(normalizedNic, metaData);
			}
			List<TableSnapshot> tableSnapshots = loadMatchingTables(metaData, normalizedNic);
			if (tableSnapshots.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found in CRIB.");
			}
			List<RowSnapshot> loanRows = new ArrayList<>();
			List<RowSnapshot> cardRows = new ArrayList<>();
			List<RowSnapshot> liabilityRows = new ArrayList<>();
			List<RowSnapshot> inquiryRows = new ArrayList<>();
			int missedPayments = 0;

			for (TableSnapshot tableSnapshot : tableSnapshots) {
				if (matchesAny(tableSnapshot.tableName(), LOAN_HINTS, tableSnapshot.columnNames())) {
					loanRows.addAll(tableSnapshot.rows());
				}
				if (matchesAny(tableSnapshot.tableName(), CARD_HINTS, tableSnapshot.columnNames())) {
					cardRows.addAll(tableSnapshot.rows());
				}
				if (matchesAny(tableSnapshot.tableName(), LIABILITY_HINTS, tableSnapshot.columnNames())) {
					liabilityRows.addAll(tableSnapshot.rows());
				}
				if (matchesAny(tableSnapshot.tableName(), INQUIRY_HINTS, tableSnapshot.columnNames())) {
					inquiryRows.addAll(tableSnapshot.rows());
				}
				missedPayments += extractMissedPayments(tableSnapshot.rows());
			}

			List<CribDatasetSnapshotResponse.CribLoanItem> loans = loanRows.stream()
				.map(this::toLoanItem)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(item -> safeString(item.loanType())))
				.toList();
			List<CribDatasetSnapshotResponse.CribCardItem> cards = cardRows.stream()
				.map(this::toCardItem)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(item -> safeString(item.provider())))
				.toList();
			List<CribDatasetSnapshotResponse.CribLiabilityItem> liabilities = liabilityRows.stream()
				.map(this::toLiabilityItem)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(item -> safeString(item.description())))
				.toList();

			BigDecimal totalActiveLoanValue = loans.stream()
				.map(CribDatasetSnapshotResponse.CribLoanItem::remainingBalance)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalCardLimit = cards.stream()
				.map(CribDatasetSnapshotResponse.CribCardItem::creditLimit)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalCardOutstanding = cards.stream()
				.map(CribDatasetSnapshotResponse.CribCardItem::outstandingBalance)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalLiabilityMonthlyAmount = liabilities.stream()
				.map(CribDatasetSnapshotResponse.CribLiabilityItem::monthlyAmount)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalMonthlyDebt = loans.stream()
				.map(CribDatasetSnapshotResponse.CribLoanItem::monthlyEmi)
				.map(this::safeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.add(totalLiabilityMonthlyAmount)
				.add(totalCardOutstanding.multiply(BigDecimal.valueOf(0.05D)));
			int activeLoansCount = loans.size();
			int inquiryCount = inquiryRows.size();
			int creditScore = computeCreditScore(
				totalMonthlyDebt,
				totalCardLimit,
				totalCardOutstanding,
				activeLoansCount,
				liabilities.size(),
				missedPayments,
				inquiryCount
			);

			return new CribDatasetSnapshotResponse(
				normalizedNic,
				properties.getSchema(),
				LocalDateTime.now(),
				creditScore,
				inquiryCount,
				activeLoansCount,
				totalActiveLoanValue,
				missedPayments,
				buildSuitabilitySummary(creditScore, activeLoansCount, cards.size(), liabilities.size()),
				loans,
				cards,
				liabilities
			);
		});
	}

	private CribDatasetSnapshotResponse lookupSnapshotUsingCribCustomerSchema(String nic, DatabaseMetaData metaData) throws SQLException {
		List<RowSnapshot> customerRows = queryRows(
			"SELECT id, nic FROM " + quoteIdentifier(TABLE_CRIB_CUSTOMERS) + " WHERE LOWER(CAST(nic AS TEXT)) = LOWER(?) LIMIT 1",
			nic
		);
		if (customerRows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ID not found in CRIB.");
		}

		RowSnapshot customerRow = customerRows.get(0);
		Long customerId = firstLong(customerRow.values(), "id");
		if (customerId == null) {
			throw new IllegalStateException("CRIB customer record is missing a valid id.");
		}

		List<RowSnapshot> loanRows = hasTable(metaData, TABLE_CRIB_LOAN_RECORDS)
			? queryRows("SELECT * FROM " + quoteIdentifier(TABLE_CRIB_LOAN_RECORDS) + " WHERE customer_id = ?", customerId)
			: List.of();
		List<RowSnapshot> cardRows = hasTable(metaData, TABLE_CRIB_CARD_RECORDS)
			? queryRows("SELECT * FROM " + quoteIdentifier(TABLE_CRIB_CARD_RECORDS) + " WHERE customer_id = ?", customerId)
			: List.of();
		List<RowSnapshot> liabilityRows = hasTable(metaData, TABLE_CRIB_OTHER_LIABILITY_RECORDS)
			? queryRows("SELECT * FROM " + quoteIdentifier(TABLE_CRIB_OTHER_LIABILITY_RECORDS) + " WHERE customer_id = ?", customerId)
			: List.of();
		RowSnapshot paymentHistoryRow = hasTable(metaData, TABLE_CRIB_PAYMENT_HISTORY)
			? querySingleRow("SELECT * FROM " + quoteIdentifier(TABLE_CRIB_PAYMENT_HISTORY) + " WHERE customer_id = ? LIMIT 1", customerId)
			: null;
		RowSnapshot creditSummaryRow = hasTable(metaData, TABLE_CRIB_CREDIT_SUMMARY)
			? querySingleRow("SELECT * FROM " + quoteIdentifier(TABLE_CRIB_CREDIT_SUMMARY) + " WHERE customer_id = ? LIMIT 1", customerId)
			: null;

		List<CribDatasetSnapshotResponse.CribLoanItem> loans = loanRows.stream()
			.map(this::toLoanItem)
			.filter(Objects::nonNull)
			.sorted(Comparator.comparing(item -> safeString(item.loanType())))
			.toList();
		List<CribDatasetSnapshotResponse.CribCardItem> cards = cardRows.stream()
			.map(this::toCardItem)
			.filter(Objects::nonNull)
			.sorted(Comparator.comparing(item -> safeString(item.provider())))
			.toList();
		List<CribDatasetSnapshotResponse.CribLiabilityItem> liabilities = liabilityRows.stream()
			.map(this::toLiabilityItem)
			.filter(Objects::nonNull)
			.sorted(Comparator.comparing(item -> safeString(item.description())))
			.toList();

		BigDecimal totalActiveLoanValueFromRows = loans.stream()
			.map(CribDatasetSnapshotResponse.CribLoanItem::remainingBalance)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardLimit = cards.stream()
			.map(CribDatasetSnapshotResponse.CribCardItem::creditLimit)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCardOutstanding = cards.stream()
			.map(CribDatasetSnapshotResponse.CribCardItem::outstandingBalance)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalLiabilityMonthlyAmount = liabilities.stream()
			.map(CribDatasetSnapshotResponse.CribLiabilityItem::monthlyAmount)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalMonthlyDebt = loans.stream()
			.map(CribDatasetSnapshotResponse.CribLoanItem::monthlyEmi)
			.map(this::safeAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add)
			.add(totalLiabilityMonthlyAmount)
			.add(totalCardOutstanding.multiply(BigDecimal.valueOf(0.05D)));

		Integer summaryLoanCount = creditSummaryRow == null
			? null
			: firstInteger(creditSummaryRow.values(), "active_loan_count", "active_facilities_count");
		Integer activeLoansCount = summaryLoanCount == null ? loans.size() : Math.max(summaryLoanCount, 0);

		BigDecimal summaryTotalActiveLoanValue = creditSummaryRow == null
			? null
			: firstBigDecimal(creditSummaryRow.values(), "total_remaining_balance", "total_loan_amount");
		BigDecimal totalActiveLoanValue = summaryTotalActiveLoanValue == null
			? totalActiveLoanValueFromRows
			: safeAmount(summaryTotalActiveLoanValue);

		Integer missedPaymentsFromHistory = paymentHistoryRow == null
			? null
			: firstInteger(paymentHistoryRow.values(), "missed_payments_last_12m", "missed_payments", "missed_payment_count");
		int missedPayments = missedPaymentsFromHistory == null ? extractMissedPayments(liabilityRows) : Math.max(missedPaymentsFromHistory, 0);

		Integer inquiryCountFromSummary = creditSummaryRow == null
			? null
			: firstInteger(creditSummaryRow.values(), "inquiry_count", "inquiries_count", "credit_inquiry_count");
		int inquiryCount = inquiryCountFromSummary == null ? 0 : Math.max(inquiryCountFromSummary, 0);

		Integer scoreFromSummary = creditSummaryRow == null ? null : firstInteger(creditSummaryRow.values(), "credit_score");
		int creditScore = scoreFromSummary == null
			? computeCreditScore(
				totalMonthlyDebt,
				totalCardLimit,
				totalCardOutstanding,
				activeLoansCount,
				liabilities.size(),
				missedPayments,
				inquiryCount
			)
			: Math.max(300, Math.min(900, scoreFromSummary));

		String riskLevel = creditSummaryRow == null ? null : firstString(creditSummaryRow.values(), "risk_level");
		String suitabilitySummary = buildSuitabilitySummaryFromRiskLevel(riskLevel, creditScore, activeLoansCount, cards.size(), liabilities.size());

		return new CribDatasetSnapshotResponse(
			nic,
			properties.getSchema(),
			LocalDateTime.now(),
			creditScore,
			inquiryCount,
			activeLoansCount,
			totalActiveLoanValue,
			missedPayments,
			suitabilitySummary,
			loans,
			cards,
			liabilities
		);
	}

	private boolean hasTable(DatabaseMetaData metaData, String tableName) throws SQLException {
		try (ResultSet tables = metaData.getTables(null, normalizeSchema(properties.getSchema()), tableName, new String[] {"TABLE"})) {
			return tables.next();
		}
	}

	private List<RowSnapshot> queryRows(String sql, Object... args) {
		return cribJdbcTemplate.query(sql, (resultSet, rowNumber) -> toRowSnapshot(resultSet), args);
	}

	private RowSnapshot querySingleRow(String sql, Object... args) {
		List<RowSnapshot> rows = queryRows(sql, args);
		return rows.isEmpty() ? null : rows.get(0);
	}

	private List<TableSnapshot> loadMatchingTables(DatabaseMetaData metaData, String nic) throws SQLException {
		List<TableSnapshot> snapshots = new ArrayList<>();
		try (ResultSet tables = metaData.getTables(null, normalizeSchema(properties.getSchema()), "%", new String[] {"TABLE"})) {
			while (tables.next()) {
				String tableName = tables.getString("TABLE_NAME");
				if (tableName == null || tableName.isBlank()) {
					continue;
				}

				Map<String, String> columnsByLowerCase = loadColumns(metaData, properties.getSchema(), tableName);
				String nicColumn = findNicColumn(columnsByLowerCase);
				if (nicColumn == null) {
					continue;
				}

				String sql = "SELECT * FROM " + quoteIdentifier(tableName) + " WHERE LOWER(CAST(" + quoteIdentifier(nicColumn) + " AS TEXT)) = LOWER(?)";
				List<RowSnapshot> rows = cribJdbcTemplate.query(sql, ps -> ps.setString(1, nic), (resultSet, rowNumber) -> toRowSnapshot(resultSet));
				if (!rows.isEmpty()) {
					snapshots.add(new TableSnapshot(tableName, columnsByLowerCase, rows));
				}
			}
		}
		return snapshots;
	}

	private Map<String, String> loadColumns(DatabaseMetaData metaData, String schema, String tableName) throws SQLException {
		Map<String, String> columnsByLowerCase = new LinkedHashMap<>();
		try (ResultSet columns = metaData.getColumns(null, normalizeSchema(schema), tableName, "%")) {
			while (columns.next()) {
				String columnName = columns.getString("COLUMN_NAME");
				if (columnName != null && !columnName.isBlank()) {
					columnsByLowerCase.put(columnName.toLowerCase(Locale.ROOT), columnName);
				}
			}
		}
		return columnsByLowerCase;
	}

	private String findNicColumn(Map<String, String> columnsByLowerCase) {
		for (Map.Entry<String, String> entry : columnsByLowerCase.entrySet()) {
			if (NIC_COLUMNS.contains(entry.getKey())) {
				return entry.getValue();
			}
		}
		for (Map.Entry<String, String> entry : columnsByLowerCase.entrySet()) {
			String lower = entry.getKey();
			if (lower.contains("nic") || lower.contains("national") || lower.contains("identity")) {
				return entry.getValue();
			}
		}
		return null;
	}

	private boolean matchesAny(String tableName, Set<String> hints, Set<String> columnNames) {
		String normalizedTableName = tableName.toLowerCase(Locale.ROOT);
		if (hints.stream().anyMatch(normalizedTableName::contains)) {
			return true;
		}
		for (String columnName : columnNames) {
			String normalizedColumn = columnName.toLowerCase(Locale.ROOT);
			if (hints.stream().anyMatch(normalizedColumn::contains)) {
				return true;
			}
		}
		return false;
	}

	private int extractMissedPayments(List<RowSnapshot> rows) {
		int total = 0;
		for (RowSnapshot row : rows) {
			Integer value = firstInteger(row.values(), "missed_payments", "missed_payment_count", "overdue_count", "default_count", "arrears_count");
			if (value != null) {
				total += value;
			}
		}
		return total;
	}

	private CribDatasetSnapshotResponse.CribLoanItem toLoanItem(RowSnapshot row) {
		String loanType = firstString(row.values(), "loan_type", "type", "facility_type", "product_type", "loan_name", "category", "facility");
		BigDecimal monthlyEmi = firstBigDecimal(row.values(), "monthly_emi", "emi", "monthly_payment", "installment", "repayment_amount", "total_monthly_emi");
		BigDecimal remainingBalance = firstBigDecimal(
			row.values(),
			"remaining_balance",
			"outstanding_balance",
			"balance",
			"principal_balance",
			"loan_amount",
			"amount",
			"total_remaining_balance"
		);
		if (loanType == null && monthlyEmi == null && remainingBalance == null) {
			return null;
		}
		return new CribDatasetSnapshotResponse.CribLoanItem(
			loanType == null ? "UNKNOWN" : loanType,
			monthlyEmi == null ? BigDecimal.ZERO : monthlyEmi,
			remainingBalance == null ? BigDecimal.ZERO : remainingBalance
		);
	}

	private CribDatasetSnapshotResponse.CribCardItem toCardItem(RowSnapshot row) {
		String provider = firstString(row.values(), "provider", "issuer", "issuer_name", "bank", "card_provider", "brand");
		BigDecimal creditLimit = firstBigDecimal(row.values(), "credit_limit", "limit", "card_limit", "approved_limit");
		BigDecimal outstandingBalance = firstBigDecimal(row.values(), "outstanding_balance", "balance", "used_amount", "current_balance");
		if (provider == null && creditLimit == null && outstandingBalance == null) {
			return null;
		}
		return new CribDatasetSnapshotResponse.CribCardItem(
			provider == null ? "UNKNOWN" : provider,
			creditLimit == null ? BigDecimal.ZERO : creditLimit,
			outstandingBalance == null ? BigDecimal.ZERO : outstandingBalance
		);
	}

	private CribDatasetSnapshotResponse.CribLiabilityItem toLiabilityItem(RowSnapshot row) {
		String description = firstString(
			row.values(),
			"description",
			"liability",
			"liability_type",
			"category",
			"type",
			"obligation",
			"name",
			"provider_name"
		);
		BigDecimal monthlyAmount = firstBigDecimal(row.values(), "monthly_amount", "monthly_payment", "installment", "emi", "payment_amount", "amount");
		if (description == null && monthlyAmount == null) {
			return null;
		}
		return new CribDatasetSnapshotResponse.CribLiabilityItem(
			description == null ? "UNKNOWN" : description,
			monthlyAmount == null ? BigDecimal.ZERO : monthlyAmount
		);
	}

	private int computeCreditScore(
		BigDecimal totalMonthlyDebt,
		BigDecimal totalCardLimit,
		BigDecimal totalCardOutstanding,
		int activeLoansCount,
		int liabilityCount,
		int missedPayments,
		int inquiryCount
	) {
		BigDecimal utilizationRatio = totalCardLimit.signum() > 0
			? totalCardOutstanding.divide(totalCardLimit, 4, RoundingMode.HALF_UP)
			: BigDecimal.ZERO;
		int score = 900;
		score -= activeLoansCount * 12;
		score -= liabilityCount * 6;
		score -= missedPayments * 20;
		score -= inquiryCount * 4;
		score -= totalMonthlyDebt.divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).intValue();
		score -= utilizationRatio.multiply(BigDecimal.valueOf(120)).setScale(0, RoundingMode.HALF_UP).intValue();
		return Math.max(300, Math.min(900, score));
	}

	private String buildSuitabilitySummary(int creditScore, int activeLoansCount, int cardCount, int liabilityCount) {
		if (creditScore >= 750) {
			return "Strong profile for loans and credit cards with manageable liabilities.";
		}
		if (creditScore >= 650) {
			return "Moderate profile suitable for standard loans and controlled card exposure.";
		}
		if (creditScore >= 550) {
			return "Caution profile. Review loan size, card limits, and liabilities before approval.";
		}
		return "High-risk profile. Additional review is required before approving loans, cards, or liabilities.";
	}

	private RowSnapshot toRowSnapshot(ResultSet resultSet) throws SQLException {
		Map<String, Object> values = new LinkedHashMap<>();
		var metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		for (int index = 1; index <= columnCount; index++) {
			String columnName = metaData.getColumnLabel(index);
			if (columnName == null || columnName.isBlank()) {
				columnName = metaData.getColumnName(index);
			}
			values.put(columnName.toLowerCase(Locale.ROOT), resultSet.getObject(index));
		}
		return new RowSnapshot(values);
	}

	private String firstString(Map<String, Object> values, String... candidates) {
		for (String candidate : candidates) {
			Object value = values.get(candidate);
			if (value != null) {
				String text = value.toString().trim();
				if (!text.isEmpty()) {
					return text;
				}
			}
		}
		return null;
	}

	private Integer firstInteger(Map<String, Object> values, String... candidates) {
		BigDecimal decimal = firstBigDecimal(values, candidates);
		return decimal == null ? null : decimal.intValue();
	}

	private Long firstLong(Map<String, Object> values, String... candidates) {
		BigDecimal decimal = firstBigDecimal(values, candidates);
		return decimal == null ? null : decimal.longValue();
	}

	private BigDecimal firstBigDecimal(Map<String, Object> values, String... candidates) {
		for (String candidate : candidates) {
			Object value = values.get(candidate);
			BigDecimal decimal = toBigDecimal(value);
			if (decimal != null) {
				return decimal;
			}
		}
		return null;
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal bigDecimal) {
			return bigDecimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		try {
			String text = value.toString().trim();
			if (text.isEmpty()) {
				return null;
			}
			return new BigDecimal(text.replace(",", ""));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private BigDecimal safeAmount(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private String normalizeSchema(String schema) {
		return schema == null || schema.isBlank() ? null : schema;
	}

	private String quoteIdentifier(String identifier) {
		return '"' + identifier.replace("\"", "\"\"") + '"';
	}

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private String buildSuitabilitySummaryFromRiskLevel(
		String riskLevel,
		int creditScore,
		int activeLoansCount,
		int cardCount,
		int liabilityCount
	) {
		if (riskLevel == null || riskLevel.isBlank()) {
			return buildSuitabilitySummary(creditScore, activeLoansCount, cardCount, liabilityCount);
		}

		String normalized = riskLevel.trim().toUpperCase(Locale.ROOT);
		if ("LOW".equals(normalized)) {
			return "Strong profile for loans and credit cards with manageable liabilities.";
		}
		if ("MEDIUM".equals(normalized)) {
			return "Moderate profile suitable for standard loans and controlled card exposure.";
		}
		if ("HIGH".equals(normalized)) {
			return "High-risk profile. Additional review is required before approving loans, cards, or liabilities.";
		}
		return buildSuitabilitySummary(creditScore, activeLoansCount, cardCount, liabilityCount);
	}

	private record TableSnapshot(String tableName, Map<String, String> columnsByLowerCase, List<RowSnapshot> rows) {
		Set<String> columnNames() {
			return columnsByLowerCase.values().stream().collect(Collectors.toSet());
		}
	}

	private record RowSnapshot(Map<String, Object> values) {
	}
}
