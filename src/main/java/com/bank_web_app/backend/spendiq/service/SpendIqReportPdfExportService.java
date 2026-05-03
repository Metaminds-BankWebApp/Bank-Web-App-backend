package com.bank_web_app.backend.spendiq.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

@Service
public class SpendIqReportPdfExportService {

	private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
	private static final float PAGE_MARGIN = 36f;
	private static final float CONTENT_WIDTH = PAGE_SIZE.getWidth() - (PAGE_MARGIN * 2);
	private static final Color PRIMARY = new Color(11, 26, 58);
	private static final Color ACCENT = new Color(37, 99, 235);
	private static final Color SUCCESS = new Color(22, 163, 74);
	private static final Color WARNING = new Color(249, 115, 22);
	private static final Color DANGER = new Color(239, 68, 68);
	private static final Color PANEL_BG = new Color(248, 250, 252);
	private static final Color PANEL_BORDER = new Color(203, 213, 225);
	private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
	private static final Color TEXT_MUTED = new Color(71, 85, 105);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu");

	public byte[] exportReport(SpendIqReportPdfModel model) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			addOverviewPage(document, model);
			addScoreAndSuggestionsPage(document, model);
			addTablePage(document, model, "Category Details", List.of("Category", "Type", "Records", "Amount"), model.categoryRows());
			addTablePage(document, model, "Budget Details", List.of("Category", "Month", "Year", "Budget"), model.budgetRows());
			addTablePage(document, model, "Income Records", List.of("Date", "Source", "Amount"), model.incomeRows());
			addTablePage(document, model, "Expense Records", List.of("Date", "Category", "Payment", "Amount"), model.expenseRows());
			document.save(output);
			return output.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to render SpendIQ PDF report.", exception);
		}
	}

	private void addOverviewPage(PDDocument document, SpendIqReportPdfModel model) throws IOException {
		PDPage page = new PDPage(PAGE_SIZE);
		document.addPage(page);
		try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
			drawHeader(stream, model, "SpendIQ Full Detail Report");
			drawMetricCard(stream, PAGE_MARGIN, 626f, 122f, "Total Income", money(model.summary().totalIncome()), SUCCESS);
			drawMetricCard(stream, PAGE_MARGIN + 134f, 626f, 122f, "Total Spend", money(model.summary().totalExpense()), DANGER);
			drawMetricCard(stream, PAGE_MARGIN + 268f, 626f, 122f, "Total Budget", money(model.summary().totalBudget()), ACCENT);
			drawMetricCard(stream, PAGE_MARGIN + 402f, 626f, 122f, "Net Savings", money(model.summary().netSavings()), model.summary().netSavings().compareTo(BigDecimal.ZERO) >= 0 ? SUCCESS : DANGER);

			drawPanel(stream, PAGE_MARGIN, 470f, 178f, 128f, new Color(239, 246, 255));
			writeText(stream, "SpendIQ Score", PDType1Font.HELVETICA_BOLD, 14f, PAGE_MARGIN + 16f, 570f, PRIMARY);
			writeText(stream, model.score() + "/100", PDType1Font.HELVETICA_BOLD, 30f, PAGE_MARGIN + 16f, 532f, TEXT_PRIMARY);
			writeText(stream, model.scoreLabel(), PDType1Font.HELVETICA_BOLD, 13f, PAGE_MARGIN + 16f, 508f, scoreColor(model.score()));
			writeText(stream, "Budget usage: " + percent(model.summary().budgetUsagePercentage()), PDType1Font.HELVETICA, 10f, PAGE_MARGIN + 16f, 488f, TEXT_MUTED);

			drawPanel(stream, PAGE_MARGIN + 202f, 470f, CONTENT_WIDTH - 202f, 128f, Color.WHITE);
			writeText(stream, "Prediction", PDType1Font.HELVETICA_BOLD, 14f, PAGE_MARGIN + 218f, 570f, PRIMARY);
			writeText(stream, model.prediction().label(), PDType1Font.HELVETICA_BOLD, 13f, PAGE_MARGIN + 218f, 546f, scoreColor(model.score()));
			writeWrappedText(stream, "Expected next month spend: " + money(model.prediction().nextMonthSpend()), PDType1Font.HELVETICA, 10f, PAGE_MARGIN + 218f, 526f, 270f, 12f, TEXT_PRIMARY);
			writeWrappedText(stream, "Expected next month savings: " + money(model.prediction().nextMonthSavings()), PDType1Font.HELVETICA, 10f, PAGE_MARGIN + 218f, 508f, 270f, 12f, TEXT_PRIMARY);
			writeWrappedText(stream, model.prediction().detail(), PDType1Font.HELVETICA, 9f, PAGE_MARGIN + 218f, 488f, 270f, 12f, TEXT_MUTED);

			drawPanel(stream, PAGE_MARGIN, 320f, CONTENT_WIDTH, 120f, PANEL_BG);
			writeText(stream, "Behavior Summary", PDType1Font.HELVETICA_BOLD, 14f, PAGE_MARGIN + 16f, 414f, PRIMARY);
			writeText(stream, "Income records: " + model.incomeRows().size(), PDType1Font.HELVETICA, 11f, PAGE_MARGIN + 16f, 388f, TEXT_PRIMARY);
			writeText(stream, "Expense records: " + model.expenseRows().size(), PDType1Font.HELVETICA, 11f, PAGE_MARGIN + 220f, 388f, TEXT_PRIMARY);
			writeText(stream, "Categories: " + model.categoryRows().size(), PDType1Font.HELVETICA, 11f, PAGE_MARGIN + 16f, 364f, TEXT_PRIMARY);
			writeText(stream, "High value transactions: " + model.highValueTransactionCount(), PDType1Font.HELVETICA, 11f, PAGE_MARGIN + 220f, 364f, TEXT_PRIMARY);
			writeText(stream, "Fixed expenses: " + money(model.fixedExpenses()), PDType1Font.HELVETICA, 11f, PAGE_MARGIN + 16f, 340f, TEXT_PRIMARY);
			writeText(stream, "Variable expenses: " + money(model.variableExpenses()), PDType1Font.HELVETICA, 11f, PAGE_MARGIN + 220f, 340f, TEXT_PRIMARY);

			drawFooter(stream, model);
		}
	}

	private void addScoreAndSuggestionsPage(PDDocument document, SpendIqReportPdfModel model) throws IOException {
		PDPage page = new PDPage(PAGE_SIZE);
		document.addPage(page);
		try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
			drawHeader(stream, model, "SpendIQ Score Details");
			float y = 652f;
			writeText(stream, "Why this score?", PDType1Font.HELVETICA_BOLD, 15f, PAGE_MARGIN, y, PRIMARY);
			y -= 24f;
			for (ScoreReasonRow reason : model.scoreReasons()) {
				drawPanel(stream, PAGE_MARGIN, y - 48f, CONTENT_WIDTH, 54f, Color.WHITE);
				writeText(stream, reason.factor(), PDType1Font.HELVETICA_BOLD, 11f, PAGE_MARGIN + 14f, y - 12f, TEXT_PRIMARY);
				writeRightAlignedText(stream, reason.pointsDeducted() > 0 ? "-" + reason.pointsDeducted() + " points" : "No penalty", PDType1Font.HELVETICA_BOLD, 10f, PAGE_MARGIN + CONTENT_WIDTH - 14f, y - 12f, reason.pointsDeducted() > 0 ? DANGER : SUCCESS);
				writeWrappedText(stream, reason.detail(), PDType1Font.HELVETICA, 9f, PAGE_MARGIN + 14f, y - 28f, CONTENT_WIDTH - 28f, 11f, TEXT_MUTED);
				y -= 68f;
			}

			y -= 8f;
			writeText(stream, "Suggestions", PDType1Font.HELVETICA_BOLD, 15f, PAGE_MARGIN, y, PRIMARY);
			y -= 24f;
			for (SuggestionRow suggestion : model.suggestions()) {
				drawPanel(stream, PAGE_MARGIN, y - 54f, CONTENT_WIDTH, 60f, PANEL_BG);
				writeText(stream, suggestion.title(), PDType1Font.HELVETICA_BOLD, 11f, PAGE_MARGIN + 14f, y - 14f, TEXT_PRIMARY);
				writeWrappedText(stream, suggestion.detail(), PDType1Font.HELVETICA, 9f, PAGE_MARGIN + 14f, y - 30f, CONTENT_WIDTH - 28f, 11f, TEXT_MUTED);
				y -= 74f;
				if (y < 96f) {
					break;
				}
			}
			drawFooter(stream, model);
		}
	}

	private void addTablePage(PDDocument document, SpendIqReportPdfModel model, String title, List<String> headers, List<? extends TableRow> rows) throws IOException {
		int pageNumber = 0;
		int rowIndex = 0;
		do {
			PDPage page = new PDPage(PAGE_SIZE);
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				drawHeader(stream, model, pageNumber == 0 ? title : title + " continued");
				float y = 656f;
				drawTableHeader(stream, headers, y);
				y -= 24f;
				if (rows.isEmpty()) {
					writeText(stream, "No records available for this report period.", PDType1Font.HELVETICA, 11f, PAGE_MARGIN + 10f, y, TEXT_MUTED);
				}
				while (rowIndex < rows.size() && y > 92f) {
					drawTableRow(stream, rows.get(rowIndex), y);
					y -= 24f;
					rowIndex++;
				}
				drawFooter(stream, model);
			}
			pageNumber++;
		} while (rowIndex < rows.size());
	}

	private void drawHeader(PDPageContentStream stream, SpendIqReportPdfModel model, String title) throws IOException {
		fillRect(stream, PAGE_MARGIN, 718f, CONTENT_WIDTH, 88f, PRIMARY);
		writeText(stream, "PrimeCore SpendIQ", PDType1Font.HELVETICA_BOLD, 12f, PAGE_MARGIN + 18f, 780f, Color.WHITE);
		writeText(stream, title, PDType1Font.HELVETICA_BOLD, 22f, PAGE_MARGIN + 18f, 750f, Color.WHITE);
		writeText(stream, model.periodLabel() + " | " + model.customerName(), PDType1Font.HELVETICA, 10f, PAGE_MARGIN + 18f, 732f, new Color(219, 234, 254));
		writeRightAlignedText(stream, "Generated " + model.generatedAtLabel(), PDType1Font.HELVETICA_BOLD, 10f, PAGE_MARGIN + CONTENT_WIDTH - 18f, 732f, new Color(219, 234, 254));
	}

	private void drawMetricCard(PDPageContentStream stream, float x, float y, float width, String title, String value, Color valueColor) throws IOException {
		drawPanel(stream, x, y, width, 74f, Color.WHITE);
		writeText(stream, title, PDType1Font.HELVETICA_BOLD, 10f, x + 12f, y + 48f, TEXT_MUTED);
		writeWrappedText(stream, value, PDType1Font.HELVETICA_BOLD, 13f, x + 12f, y + 26f, width - 24f, 12f, valueColor);
	}

	private void drawTableHeader(PDPageContentStream stream, List<String> headers, float y) throws IOException {
		fillRect(stream, PAGE_MARGIN, y, CONTENT_WIDTH, 22f, new Color(226, 232, 240));
		float[] xPositions = { PAGE_MARGIN + 10f, PAGE_MARGIN + 176f, PAGE_MARGIN + 310f, PAGE_MARGIN + 414f };
		for (int index = 0; index < headers.size(); index++) {
			writeText(stream, headers.get(index), PDType1Font.HELVETICA_BOLD, 9f, xPositions[Math.min(index, xPositions.length - 1)], y + 7f, TEXT_MUTED);
		}
	}

	private void drawTableRow(PDPageContentStream stream, TableRow row, float y) throws IOException {
		strokeRect(stream, PAGE_MARGIN, y - 2f, CONTENT_WIDTH, 22f, PANEL_BORDER);
		float[] xPositions = { PAGE_MARGIN + 10f, PAGE_MARGIN + 176f, PAGE_MARGIN + 310f, PAGE_MARGIN + 414f };
		List<String> values = row.values();
		for (int index = 0; index < values.size(); index++) {
			writeText(stream, truncate(values.get(index), index == 0 ? 26 : 18), PDType1Font.HELVETICA, 8.5f, xPositions[Math.min(index, xPositions.length - 1)], y + 5f, TEXT_PRIMARY);
		}
	}

	private void drawFooter(PDPageContentStream stream, SpendIqReportPdfModel model) throws IOException {
		writeText(stream, "This file is generated by the backend from stored SpendIQ records for the authenticated user.", PDType1Font.HELVETICA, 8.5f, PAGE_MARGIN, 44f, TEXT_MUTED);
		writeRightAlignedText(stream, model.periodLabel(), PDType1Font.HELVETICA, 8.5f, PAGE_MARGIN + CONTENT_WIDTH, 44f, TEXT_MUTED);
	}

	private void drawPanel(PDPageContentStream stream, float x, float y, float width, float height, Color background) throws IOException {
		fillRect(stream, x, y, width, height, background);
		strokeRect(stream, x, y, width, height, PANEL_BORDER);
	}

	private void fillRect(PDPageContentStream stream, float x, float y, float width, float height, Color color) throws IOException {
		stream.setNonStrokingColor(color);
		stream.addRect(x, y, width, height);
		stream.fill();
	}

	private void strokeRect(PDPageContentStream stream, float x, float y, float width, float height, Color color) throws IOException {
		stream.setStrokingColor(color);
		stream.setLineWidth(0.8f);
		stream.addRect(x, y, width, height);
		stream.stroke();
	}

	private void writeText(PDPageContentStream stream, String text, PDFont font, float fontSize, float x, float y, Color color) throws IOException {
		stream.beginText();
		stream.setFont(font, fontSize);
		stream.setNonStrokingColor(color);
		stream.newLineAtOffset(x, y);
		stream.showText(safe(text));
		stream.endText();
	}

	private void writeRightAlignedText(PDPageContentStream stream, String text, PDFont font, float fontSize, float rightX, float y, Color color) throws IOException {
		writeText(stream, text, font, fontSize, rightX - textWidth(font, fontSize, text), y, color);
	}

	private void writeWrappedText(PDPageContentStream stream, String text, PDFont font, float fontSize, float x, float y, float maxWidth, float lineHeight, Color color) throws IOException {
		float currentY = y;
		for (String line : wrapText(text, font, fontSize, maxWidth)) {
			writeText(stream, line, font, fontSize, x, currentY, color);
			currentY -= lineHeight;
		}
	}

	private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
		List<String> lines = new ArrayList<>();
		StringBuilder currentLine = new StringBuilder();
		for (String word : safe(text).split("\\s+")) {
			String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
			if (textWidth(font, fontSize, candidate) <= maxWidth || currentLine.length() == 0) {
				currentLine = new StringBuilder(candidate);
			} else {
				lines.add(currentLine.toString());
				currentLine = new StringBuilder(word);
			}
		}
		if (currentLine.length() > 0) {
			lines.add(currentLine.toString());
		}
		return lines;
	}

	private float textWidth(PDFont font, float fontSize, String text) throws IOException {
		return font.getStringWidth(safe(text)) / 1000f * fontSize;
	}

	private Color scoreColor(int score) {
		if (score >= 75) {
			return SUCCESS;
		}
		if (score >= 50) {
			return WARNING;
		}
		return DANGER;
	}

	private String money(BigDecimal value) {
		DecimalFormat format = new DecimalFormat("#,##0.00");
		return "LKR " + format.format(value == null ? BigDecimal.ZERO : value);
	}

	private String percent(BigDecimal value) {
		return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
	}

	private String truncate(String value, int maxLength) {
		String safeValue = safe(value);
		if (safeValue.length() <= maxLength) {
			return safeValue;
		}
		return safeValue.substring(0, Math.max(0, maxLength - 3)) + "...";
	}

	private String safe(String value) {
		return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "").trim();
	}

	public interface TableRow {
		List<String> values();
	}

	public record SpendIqReportPdfModel(
		String customerName,
		String periodLabel,
		String generatedAtLabel,
		SummaryRow summary,
		int score,
		String scoreLabel,
		BigDecimal fixedExpenses,
		BigDecimal variableExpenses,
		int highValueTransactionCount,
		List<ScoreReasonRow> scoreReasons,
		List<SuggestionRow> suggestions,
		PredictionRow prediction,
		List<CategoryRow> categoryRows,
		List<BudgetRow> budgetRows,
		List<IncomeRow> incomeRows,
		List<ExpenseRow> expenseRows
	) {
		public String generatedAtLabel() {
			if (generatedAtLabel == null || generatedAtLabel.isBlank()) {
				return DATE_FORMATTER.format(LocalDate.now());
			}
			return generatedAtLabel;
		}
	}

	public record SummaryRow(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal totalBudget, BigDecimal netSavings, BigDecimal remainingBudget, BigDecimal budgetUsagePercentage) {}

	public record ScoreReasonRow(String factor, int pointsDeducted, String detail) {}

	public record SuggestionRow(String title, String detail) {}

	public record PredictionRow(String label, BigDecimal nextMonthSpend, BigDecimal nextMonthSavings, String detail) {}

	public record CategoryRow(String category, String type, int count, BigDecimal amount) implements TableRow {
		@Override
		public List<String> values() {
			return List.of(category, type, String.valueOf(count), amount == null ? "0.00" : amount.toPlainString());
		}
	}

	public record BudgetRow(String category, int month, int year, BigDecimal amount) implements TableRow {
		@Override
		public List<String> values() {
			return List.of(category, String.valueOf(month), String.valueOf(year), amount == null ? "0.00" : amount.toPlainString());
		}
	}

	public record IncomeRow(String date, String source, BigDecimal amount) implements TableRow {
		@Override
		public List<String> values() {
			return List.of(date, source, amount == null ? "0.00" : amount.toPlainString());
		}
	}

	public record ExpenseRow(String date, String category, String paymentType, BigDecimal amount) implements TableRow {
		@Override
		public List<String> values() {
			return List.of(date, category, paymentType, amount == null ? "0.00" : amount.toPlainString());
		}
	}
}
