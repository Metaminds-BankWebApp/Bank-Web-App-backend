package com.bank_web_app.backend.transact.service;

import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.BankCustomerRepository;
import com.bank_web_app.backend.transact.entity.Transaction;
import com.bank_web_app.backend.transact.repository.TransactionRepository;
import com.bank_web_app.backend.user.entity.User;
import com.bank_web_app.backend.user.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Generates a downloadable receipt for a completed transaction owned by the logged-in customer.
@Service
public class TransactionReceiptPdfService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String STATUS_SUCCESS = "SUCCESS";
	private static final Color BRAND_PRIMARY = new Color(6, 30, 61);
	private static final Color BRAND_ACCENT = new Color(0, 150, 136);
	private static final Color DETAIL_BACKGROUND = new Color(248, 250, 252);
	private static final Color DETAIL_BORDER = new Color(207, 224, 245);
	private static final Font BRAND_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.WHITE);
	private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BRAND_PRIMARY);
	private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(71, 85, 105));
	private static final Font LABEL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139));
	private static final Font VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(30, 41, 59));
	private static final Font REFERENCE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND_ACCENT);
	private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(100, 116, 139));
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

	private final TransactionRepository transactionRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final UserRepository userRepository;

	public TransactionReceiptPdfService(
		TransactionRepository transactionRepository,
		BankCustomerRepository bankCustomerRepository,
		UserRepository userRepository
	) {
		this.transactionRepository = transactionRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public ReceiptPdfResult generateReceiptPdf(String referenceNo) {
		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		String normalizedReference = safeText(referenceNo);
		Transaction transaction = transactionRepository
			.findByReferenceNoAndBankCustomer_BankCustomerId(normalizedReference, bankCustomer.getBankCustomerId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction receipt was not found."));

		if (!STATUS_SUCCESS.equalsIgnoreCase(safeText(transaction.getStatus()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A receipt is available only for successful transactions.");
		}

		String fileName = "transaction-receipt-" + normalizedReference.replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
		return new ReceiptPdfResult(fileName, buildPdf(transaction, bankCustomer));
	}

	private byte[] buildPdf(Transaction transaction, BankCustomer bankCustomer) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			Document document = new Document(PageSize.A4, 54f, 54f, 46f, 48f);
			PdfWriter.getInstance(document, output);
			document.open();

			addBrandBanner(document);
			addTitle(document);
			addReceiptDetails(document, transaction, bankCustomer);
			addFooter(document);

			document.close();
			return output.toByteArray();
		} catch (DocumentException exception) {
			throw new IllegalStateException("Failed to generate transaction receipt PDF.", exception);
		}
	}

	private void addBrandBanner(Document document) throws DocumentException {
		PdfPTable banner = new PdfPTable(1);
		banner.setWidthPercentage(100f);
		PdfPCell cell = new PdfPCell(new Phrase("PRIMECORE DIGITAL BANK", BRAND_FONT));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setBackgroundColor(BRAND_PRIMARY);
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setPadding(11f);
		banner.addCell(cell);
		document.add(banner);
	}

	private void addTitle(Document document) throws DocumentException {
		Paragraph title = new Paragraph("Transaction Successful", TITLE_FONT);
		title.setAlignment(Element.ALIGN_CENTER);
		title.setSpacingBefore(36f);
		title.setSpacingAfter(7f);
		document.add(title);

		Paragraph subtitle = new Paragraph("Fund Transfer Receipt", SUBTITLE_FONT);
		subtitle.setAlignment(Element.ALIGN_CENTER);
		subtitle.setSpacingAfter(25f);
		document.add(subtitle);
	}

	private void addReceiptDetails(Document document, Transaction transaction, BankCustomer bankCustomer) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[] { 1.25f, 1.75f });
		table.setWidthPercentage(100f);
		table.setSpacingAfter(22f);

		LocalDateTime transactionDate = transaction.getTransactionDate();
		addDetailRow(table, "REFERENCE NO", safeText(transaction.getReferenceNo()), true);
		addDetailRow(table, "TRANSACTION STATUS", "SUCCESS", true);
		addDetailRow(table, "TRANSACTION AMOUNT", "LKR " + formatAmount(transaction.getAmount()), false);
		addDetailRow(table, "TRANSACTION DATE", transactionDate == null ? "-" : transactionDate.format(DATE_FORMATTER), false);
		addDetailRow(table, "TRANSACTION TIME", transactionDate == null ? "-" : transactionDate.format(TIME_FORMATTER), false);
		addDetailRow(table, "SENDER NAME", resolveDisplayName(bankCustomer.getUser()), false);
		addDetailRow(table, "SOURCE ACCOUNT", safeText(transaction.getSenderAccountNo()), false);
		addDetailRow(table, "RECEIVER NAME", safeText(transaction.getReceiverName()), false);
		addDetailRow(table, "RECEIVER ACCOUNT", safeText(transaction.getReceiverAccountNo()), false);
		addDetailRow(table, "DESCRIPTION", safeText(transaction.getRemark()), false);

		document.add(table);
	}

	private void addDetailRow(PdfPTable table, String label, String value, boolean accentValue) {
		PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
		labelCell.setBackgroundColor(DETAIL_BACKGROUND);
		labelCell.setBorderColor(DETAIL_BORDER);
		labelCell.setPadding(9f);
		labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(labelCell);

		PdfPCell valueCell = new PdfPCell(new Phrase(value.isBlank() ? "-" : value, accentValue ? REFERENCE_FONT : VALUE_FONT));
		valueCell.setBackgroundColor(Color.WHITE);
		valueCell.setBorderColor(DETAIL_BORDER);
		valueCell.setPadding(9f);
		valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(valueCell);
	}

	private void addFooter(Document document) throws DocumentException {
		Paragraph confirmation = new Paragraph("Transaction has been completed successfully.", SUBTITLE_FONT);
		confirmation.setAlignment(Element.ALIGN_CENTER);
		confirmation.setSpacingBefore(8f);
		confirmation.setSpacingAfter(10f);
		document.add(confirmation);

		Paragraph footer = new Paragraph("This is a computer-generated receipt. Signature is not required.", FOOTER_FONT);
		footer.setAlignment(Element.ALIGN_CENTER);
		document.add(footer);
	}

	private String formatAmount(BigDecimal amount) {
		DecimalFormat formatter = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		return formatter.format(amount == null ? BigDecimal.ZERO : amount);
	}

	private String resolveDisplayName(User user) {
		if (user == null) {
			return "Bank Customer";
		}
		String fullName = (safeText(user.getFirstName()) + " " + safeText(user.getLastName())).trim();
		if (!fullName.isBlank()) {
			return fullName;
		}
		String username = safeText(user.getUsername());
		return username.isBlank() ? safeText(user.getEmail()) : username;
	}

	private BankCustomer resolveLoggedInBankCustomer() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (
			authentication == null ||
			!authentication.isAuthenticated() ||
			authentication instanceof AnonymousAuthenticationToken ||
			authentication.getName() == null ||
			authentication.getName().isBlank()
		) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bank customer authentication is required.");
		}

		String principal = authentication.getName().trim();
		String normalizedPrincipal = principal.toLowerCase(Locale.ROOT);
		User user = userRepository
			.findByEmail(normalizedPrincipal)
			.or(() -> userRepository.findByUsername(principal))
			.or(() -> userRepository.findByUsername(normalizedPrincipal))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Logged-in user was not found."));

		String roleName = user.getRole() == null || user.getRole().getRoleName() == null
			? ""
			: user.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);
		if (!ROLE_BANK_CUSTOMER.equals(roleName)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Logged-in user is not a bank customer.");
		}

		return bankCustomerRepository
			.findByUser_UserId(user.getUserId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Bank customer profile was not found for logged-in user."));
	}

	private String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	public record ReceiptPdfResult(String fileName, byte[] content) {}
}
