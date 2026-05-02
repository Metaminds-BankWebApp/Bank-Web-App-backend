package com.bank_web_app.backend.creditlens.service;

import com.bank_web_app.backend.creditlens.dto.response.CreditRiskFactorResponse;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

@Service
public class CreditReportPdfExportService {

	private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
	private static final float PAGE_MARGIN = 36f;
	private static final float CONTENT_WIDTH = PAGE_SIZE.getWidth() - (PAGE_MARGIN * 2);
	private static final float CARD_GAP = 12f;
	private static final Color PRIMARY = new Color(8, 57, 100);
	private static final Color PRIMARY_ACCENT = new Color(43, 122, 183);
	private static final Color PANEL_BORDER = new Color(212, 224, 236);
	private static final Color PANEL_BG = new Color(247, 250, 252);
	private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
	private static final Color TEXT_MUTED = new Color(71, 85, 105);
	private static final Color LOW_RISK = new Color(34, 197, 94);
	private static final Color MEDIUM_RISK = new Color(245, 158, 11);
	private static final Color HIGH_RISK = new Color(239, 68, 68);
	private static final DateTimeFormatter FOOTER_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM uuuu, hh:mm a", Locale.ENGLISH);

	public byte[] exportPublicCustomerReport(PublicCreditReportPdfModel model) {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage(PAGE_SIZE);
			document.addPage(page);

			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				drawHeader(stream, model);
				drawScorePanel(stream, model);
				drawBehaviorPanel(stream, model);
				drawFinancialCards(stream, model);
				drawRiskFactorTable(stream, model);
				drawFooter(stream, model);
			}

			document.save(output);
			return output.toByteArray();
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to render CreditLens PDF report.", exception);
		}
	}

	private void drawHeader(PDPageContentStream stream, PublicCreditReportPdfModel model) throws IOException {
		float x = PAGE_MARGIN;
		float y = 712f;
		float height = 94f;
		float leftPadding = 18f;
		float rightSectionWidth = 150f;
		float rightSectionX = x + CONTENT_WIDTH - rightSectionWidth - leftPadding;
		float leftTextWidth = rightSectionX - (x + leftPadding) - 16f;

		fillRect(stream, x, y, CONTENT_WIDTH, height, PRIMARY);
		writeText(stream, "PrimeCore CreditLens", PDType1Font.HELVETICA_BOLD, 12f, x + leftPadding, y + 66f, Color.WHITE);
		writeText(stream, "Credit Risk Report", PDType1Font.HELVETICA_BOLD, 24f, x + leftPadding, y + 36f, Color.WHITE);
		writeWrappedText(
			stream,
			"Monthly public customer snapshot generated from your CreditLens evaluation.",
			PDType1Font.HELVETICA,
			11f,
			x + leftPadding,
			y + 18f,
			leftTextWidth,
			12f,
			new Color(219, 234, 254)
		);

		drawBadge(stream, x + CONTENT_WIDTH - 110f - leftPadding, y + 58f, 110f, 24f, model.riskLabel() + " Risk", resolveRiskColor(model.riskLabel()), Color.WHITE);
		writeRightAlignedText(
			stream,
			"Month: " + safe(model.monthLabel()),
			PDType1Font.HELVETICA_BOLD,
			12f,
			rightSectionX + rightSectionWidth,
			y + 24f,
			new Color(219, 234, 254)
		);
	}

	private void drawScorePanel(PDPageContentStream stream, PublicCreditReportPdfModel model) throws IOException {
		float x = PAGE_MARGIN;
		float y = 598f;
		float width = 178f;
		float height = 108f;

		drawPanel(stream, x, y, width, height, new Color(238, 246, 255));
		writeText(stream, "Credit Risk Score", PDType1Font.HELVETICA_BOLD, 13f, x + 16f, y + 84f, PRIMARY);
		writeText(stream, safe(model.score()) + "/100", PDType1Font.HELVETICA_BOLD, 28f, x + 16f, y + 48f, TEXT_PRIMARY);
		writeText(stream, safe(model.riskLabel()) + " Risk", PDType1Font.HELVETICA_BOLD, 13f, x + 16f, y + 28f, resolveRiskColor(model.riskLabel()));
		writeText(stream, "Evaluation: " + safe(model.evaluationType()), PDType1Font.HELVETICA, 10f, x + 16f, y + 12f, TEXT_MUTED);
	}

	private void drawBehaviorPanel(PDPageContentStream stream, PublicCreditReportPdfModel model) throws IOException {
		float x = PAGE_MARGIN + 190f;
		float y = 598f;
		float width = CONTENT_WIDTH - 190f;
		float height = 108f;

		drawPanel(stream, x, y, width, height, Color.WHITE);
		writeText(stream, "Profile Overview", PDType1Font.HELVETICA_BOLD, 13f, x + 16f, y + 84f, PRIMARY);
		writeText(stream, safe(model.customerName()), PDType1Font.HELVETICA_BOLD, 16f, x + 16f, y + 60f, TEXT_PRIMARY);
		writeText(stream, "Customer Code: " + safe(model.customerCode()), PDType1Font.HELVETICA, 10f, x + 16f, y + 45f, TEXT_MUTED);

		writeText(stream, "DTI: " + formatPercentage(model.dtiPercentage()) + " (" + safe(model.dtiLabel()) + ")", PDType1Font.HELVETICA, 11f, x + 16f, y + 26f, TEXT_PRIMARY);
		writeText(stream, "Utilization: " + formatPercentage(model.utilizationPercentage()), PDType1Font.HELVETICA, 11f, x + 210f, y + 26f, TEXT_PRIMARY);
		writeText(stream, "Missed Payments: " + safe(model.missedPayments()), PDType1Font.HELVETICA, 11f, x + 16f, y + 12f, TEXT_PRIMARY);
		writeText(stream, "Active Facilities: " + safe(model.activeFacilities()), PDType1Font.HELVETICA, 11f, x + 210f, y + 12f, TEXT_PRIMARY);
	}

	private void drawFinancialCards(PDPageContentStream stream, PublicCreditReportPdfModel model) throws IOException {
		float cardWidth = (CONTENT_WIDTH - CARD_GAP) / 2f;
		float topRowY = 488f;
		float bottomRowY = 404f;
		float cardHeight = 72f;

		drawMetricCard(stream, PAGE_MARGIN, topRowY, cardWidth, cardHeight, "Monthly Income", formatCurrency(model.monthlyIncome()), null, new Color(236, 253, 245));
		drawMetricCard(stream, PAGE_MARGIN + cardWidth + CARD_GAP, topRowY, cardWidth, cardHeight, "Loan", "EMI: " + formatCurrency(model.loanEmi()), "Remaining Balance: " + formatCurrency(model.loanRemainingBalance()), new Color(255, 247, 237));
		drawMetricCard(stream, PAGE_MARGIN, bottomRowY, cardWidth, cardHeight, "Credit Card", formatCurrency(model.creditCardBalance()), "Limit: " + formatCurrency(model.creditCardLimit()), new Color(239, 246, 255));
		drawMetricCard(stream, PAGE_MARGIN + cardWidth + CARD_GAP, bottomRowY, cardWidth, cardHeight, "Other Liabilities", formatCurrency(model.otherLiabilities()), null, new Color(245, 243, 255));
	}

	private void drawMetricCard(
		PDPageContentStream stream,
		float x,
		float y,
		float width,
		float height,
		String title,
		String primaryValue,
		String secondaryValue,
		Color background
	) throws IOException {
		drawPanel(stream, x, y, width, height, background);
		writeText(stream, title, PDType1Font.HELVETICA_BOLD, 12f, x + 14f, y + 52f, PRIMARY);
		writeText(stream, primaryValue, PDType1Font.HELVETICA_BOLD, 15f, x + 14f, y + 30f, TEXT_PRIMARY);
		if (!safe(secondaryValue).isBlank()) {
			writeText(stream, secondaryValue, PDType1Font.HELVETICA, 10f, x + 14f, y + 14f, TEXT_MUTED);
		}
	}

	private void drawRiskFactorTable(PDPageContentStream stream, PublicCreditReportPdfModel model) throws IOException {
		float x = PAGE_MARGIN;
		float y = 156f;
		float width = CONTENT_WIDTH;
		float height = 210f;

		drawPanel(stream, x, y, width, height, Color.WHITE);
		writeText(stream, "Risk Points Breakdown", PDType1Font.HELVETICA_BOLD, 14f, x + 16f, y + height - 24f, PRIMARY);
		writeText(stream, "Each factor contributes to the total CreditLens risk score.", PDType1Font.HELVETICA, 10f, x + 16f, y + height - 40f, TEXT_MUTED);

		float tableTop = y + height - 62f;
		drawTableHeader(stream, x + 16f, tableTop, width - 32f);

		float rowY = tableTop - 28f;
		for (CreditRiskFactorResponse factor : model.factors()) {
			drawTableRow(stream, x + 16f, rowY, width - 32f, factor);
			rowY -= 28f;
		}
	}

	private void drawTableHeader(PDPageContentStream stream, float x, float y, float width) throws IOException {
		fillRect(stream, x, y, width, 20f, new Color(241, 245, 249));
		writeText(stream, "Factor", PDType1Font.HELVETICA_BOLD, 10f, x + 10f, y + 6f, TEXT_MUTED);
		writeText(stream, "Points", PDType1Font.HELVETICA_BOLD, 10f, x + width - 170f, y + 6f, TEXT_MUTED);
		writeText(stream, "Band", PDType1Font.HELVETICA_BOLD, 10f, x + width - 78f, y + 6f, TEXT_MUTED);
	}

	private void drawTableRow(PDPageContentStream stream, float x, float y, float width, CreditRiskFactorResponse factor) throws IOException {
		strokeRect(stream, x, y, width, 24f, PANEL_BORDER);
		writeText(stream, safe(factor.name()), PDType1Font.HELVETICA, 11f, x + 10f, y + 7f, TEXT_PRIMARY);
		writeText(stream, safe(factor.value()) + " / " + safe(factor.max()), PDType1Font.HELVETICA_BOLD, 11f, x + width - 162f, y + 7f, TEXT_PRIMARY);
		String band = resolveFactorBand(factor.value(), factor.max());
		drawBadge(stream, x + width - 84f, y + 4f, 66f, 16f, band, resolveBandColor(band), Color.WHITE);
	}

	private void drawFooter(PDPageContentStream stream, PublicCreditReportPdfModel model) throws IOException {
		float x = PAGE_MARGIN;
		float y = 56f;
		float height = 76f;

		drawPanel(stream, x, y, CONTENT_WIDTH, height, PANEL_BG);
		writeText(stream, "Generated on " + safe(model.generatedAtLabel()), PDType1Font.HELVETICA_BOLD, 10f, x + 14f, y + 52f, TEXT_PRIMARY);
		writeText(stream, "Last evaluation update: " + safe(model.lastUpdatedLabel()), PDType1Font.HELVETICA, 10f, x + 14f, y + 38f, TEXT_MUTED);
		writeWrappedText(
			stream,
			"This CreditLens PDF is generated from the customer's stored financial record and evaluation factors. Use it as an informational report alongside the in-app dashboard and insights.",
			PDType1Font.HELVETICA,
			9f,
			x + 14f,
			y + 22f,
			CONTENT_WIDTH - 28f,
			12f,
			TEXT_MUTED
		);
	}

	private void drawPanel(PDPageContentStream stream, float x, float y, float width, float height, Color background) throws IOException {
		fillRect(stream, x, y, width, height, background);
		strokeRect(stream, x, y, width, height, PANEL_BORDER);
	}

	private void drawBadge(PDPageContentStream stream, float x, float y, float width, float height, String text, Color background, Color textColor) throws IOException {
		fillRect(stream, x, y, width, height, background);
		float textWidth = textWidth(PDType1Font.HELVETICA_BOLD, 9f, text);
		writeText(stream, text, PDType1Font.HELVETICA_BOLD, 9f, x + Math.max(8f, (width - textWidth) / 2f), y + 6f, textColor);
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

	private void writeRightAlignedText(
		PDPageContentStream stream,
		String text,
		PDFont font,
		float fontSize,
		float rightX,
		float y,
		Color color
	) throws IOException {
		float x = rightX - textWidth(font, fontSize, text);
		writeText(stream, text, font, fontSize, x, y, color);
	}

	private void writeWrappedText(
		PDPageContentStream stream,
		String text,
		PDFont font,
		float fontSize,
		float x,
		float y,
		float maxWidth,
		float lineHeight,
		Color color
	) throws IOException {
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

	private String resolveFactorBand(Integer value, Integer max) {
		int safeValue = value == null ? 0 : value;
		int safeMax = Math.max(1, max == null ? 0 : max);
		BigDecimal ratio = BigDecimal.valueOf(safeValue)
			.divide(BigDecimal.valueOf(safeMax), 4, RoundingMode.HALF_UP);
		if (ratio.compareTo(BigDecimal.ZERO) <= 0) {
			return "LOW";
		}
		if (ratio.compareTo(BigDecimal.ONE) >= 0) {
			return "MAX";
		}
		if (ratio.compareTo(new BigDecimal("0.66")) >= 0) {
			return "HIGH";
		}
		if (ratio.compareTo(new BigDecimal("0.33")) >= 0) {
			return "MEDIUM";
		}
		return "LOW";
	}

	private Color resolveRiskColor(String riskLabel) {
		String normalized = safe(riskLabel).toUpperCase(Locale.ROOT);
		if ("HIGH".equals(normalized)) {
			return HIGH_RISK;
		}
		if ("MEDIUM".equals(normalized)) {
			return MEDIUM_RISK;
		}
		return LOW_RISK;
	}

	private Color resolveBandColor(String band) {
		return switch (safe(band).toUpperCase(Locale.ROOT)) {
			case "MAX", "HIGH" -> HIGH_RISK;
			case "MEDIUM" -> MEDIUM_RISK;
			default -> LOW_RISK;
		};
	}

	private String formatCurrency(BigDecimal value) {
		DecimalFormat format = new DecimalFormat("#,##0.00");
		return "LKR " + format.format(value == null ? BigDecimal.ZERO : value);
	}

	private String formatPercentage(BigDecimal value) {
		BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
		return safeValue.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
	}

	private String safe(String value) {
		return value == null ? "" : value.trim();
	}

	private String safe(Integer value) {
		return value == null ? "0" : value.toString();
	}

	public record PublicCreditReportPdfModel(
		String customerName,
		String customerCode,
		String monthLabel,
		String evaluationType,
		Integer score,
		String riskLabel,
		String generatedAtLabel,
		String lastUpdatedLabel,
		BigDecimal monthlyIncome,
		BigDecimal loanEmi,
		BigDecimal loanRemainingBalance,
		BigDecimal creditCardBalance,
		BigDecimal creditCardLimit,
		BigDecimal otherLiabilities,
		Integer missedPayments,
		Integer activeFacilities,
		BigDecimal dtiPercentage,
		BigDecimal utilizationPercentage,
		String dtiLabel,
		List<CreditRiskFactorResponse> factors
	) {
		public String generatedAtLabel() {
			if (generatedAtLabel == null || generatedAtLabel.isBlank()) {
				return FOOTER_TIME_FORMATTER.format(java.time.LocalDateTime.now());
			}
			return generatedAtLabel;
		}
	}
}


