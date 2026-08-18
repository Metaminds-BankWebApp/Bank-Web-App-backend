package com.bank_web_app.backend.transact.service;

import com.bank_web_app.backend.bankcustomer.entity.Account;
import com.bank_web_app.backend.bankcustomer.entity.BankCustomer;
import com.bank_web_app.backend.bankcustomer.repository.AccountRepository;
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
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionStatementPdfService {

	private static final String ROLE_BANK_CUSTOMER = "BANK_CUSTOMER";
	private static final String STATUS_SUCCESS = "SUCCESS";
	private static final DateTimeFormatter STATEMENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM", Locale.ENGLISH);
	private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

	private static final Color BRAND_PRIMARY = new Color(11, 62, 90);
	private static final Color BRAND_SECONDARY = new Color(14, 79, 98);
	private static final Color BRAND_ACCENT = new Color(57, 159, 216);
	private static final Color ROW_BG = new Color(236, 245, 252);
	private static final Color ROW_BG_ALT = new Color(245, 250, 255);
	private static final Color TOTAL_VALUE_BG = new Color(244, 250, 255);
	private static final Color SECTION_BG = new Color(248, 252, 255);
	private static final Color SECTION_BORDER = new Color(188, 215, 234);
	private static final Color WHITE_GRID = new Color(255, 255, 255);

	private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 10);
	private static final Font FONT_SMALL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
	private static final Font FONT_SMALL_BOLD_WHITE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
	private static final Font FONT_TABLE_HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
	private static final Font FONT_TABLE_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9);
	private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
	private static final Font FONT_TITLE_WHITE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
	private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
	private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 8);
	private static final Font FONT_FOOTER_ITALIC = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

	private final TransactionRepository transactionRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;

	// Injects dependencies required for statement data resolution and auth context.
	public TransactionStatementPdfService(
		TransactionRepository transactionRepository,
		BankCustomerRepository bankCustomerRepository,
		AccountRepository accountRepository,
		UserRepository userRepository
	) {
		this.transactionRepository = transactionRepository;
		this.bankCustomerRepository = bankCustomerRepository;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
	}

	// Generates statement PDF bytes (and file name) for selected or default date range.
	@Transactional(readOnly = true)
	public StatementPdfResult generateStatementPdf(LocalDate fromDate, LocalDate toDate) {
		DateRange range = resolveDateRange(fromDate, toDate);

		BankCustomer bankCustomer = resolveLoggedInBankCustomer();
		Account account = resolveOwnedAccountForBankCustomer(bankCustomer);
		String accountNumber = normalizeAccountNumber(account.getAccountNumber());
		if (accountNumber.isBlank()) {
			throw new IllegalStateException("Account number is invalid for logged-in bank customer.");
		}

		LocalDateTime fromDateTime = range.fromDate().atStartOfDay();
		LocalDateTime toDateTime = range.toDate().atTime(LocalTime.MAX);

		List<Transaction> allSuccessTransactionsDesc = transactionRepository.findAllByAccountNoAndStatusOrderByTransactionDateDesc(
			accountNumber,
			STATUS_SUCCESS
		);
		List<Transaction> periodTransactions = transactionRepository.findAllByAccountNoAndStatusBetweenDatesOrderByTransactionDateAsc(
			accountNumber,
			STATUS_SUCCESS,
			fromDateTime,
			toDateTime
		);

		BigDecimal openingBalance = computeOpeningBalance(
			fromDateTime,
			accountNumber,
			safeAmount(account.getBalance()),
			allSuccessTransactionsDesc
		);
		StatementComputation computation = computeStatementRows(accountNumber, openingBalance, periodTransactions, range);
		byte[] pdfContent = buildPdf(bankCustomer, account, range, computation);

		String fileName =
			"transaction-statement-" +
			accountNumber +
			"-" +
			range.fromDate().format(FILE_DATE_FORMATTER) +
			"-" +
			range.toDate().format(FILE_DATE_FORMATTER) +
			".pdf";
		return new StatementPdfResult(fileName, pdfContent);
	}

	// Resolves effective date range and validates ordering + future constraints.
	private DateRange resolveDateRange(LocalDate fromDate, LocalDate toDate) {
		LocalDate today = LocalDate.now();
		LocalDate effectiveToDate = toDate == null ? today : toDate;
		LocalDate effectiveFromDate = fromDate == null ? effectiveToDate.withDayOfMonth(1) : fromDate;

		if (effectiveFromDate.isAfter(effectiveToDate)) {
			throw new IllegalArgumentException("fromDate must be before or equal to toDate.");
		}
		if (effectiveFromDate.isAfter(today) || effectiveToDate.isAfter(today)) {
			throw new IllegalArgumentException("Date range cannot be in the future.");
		}

		return new DateRange(effectiveFromDate, effectiveToDate);
	}

	// Computes opening balance at range start by reversing later successful transactions.
	private BigDecimal computeOpeningBalance(
		LocalDateTime fromDateTime,
		String accountNumber,
		BigDecimal currentBalance,
		List<Transaction> allSuccessTransactionsDesc
	) {
		BigDecimal runningBalance = currentBalance;
		for (Transaction transaction : allSuccessTransactionsDesc) {
			LocalDateTime transactionDate = transaction.getTransactionDate();
			if (transactionDate == null || transactionDate.isBefore(fromDateTime)) {
				break;
			}
			runningBalance = reverseTransactionEffect(runningBalance, transaction, accountNumber);
		}
		return runningBalance;
	}

	// Builds statement rows and aggregate deposit/withdrawal totals for the period.
	private StatementComputation computeStatementRows(
		String accountNumber,
		BigDecimal openingBalance,
		List<Transaction> periodTransactions,
		DateRange range
	) {
		List<StatementRow> rows = new ArrayList<>();
		rows.add(new StatementRow(range.fromDate().format(TABLE_DATE_FORMATTER), "BALANCE B/F", null, null, openingBalance));

		BigDecimal runningBalance = openingBalance;
		BigDecimal totalDeposits = BigDecimal.ZERO;
		BigDecimal totalWithdrawals = BigDecimal.ZERO;
		int depositCount = 0;
		int withdrawalCount = 0;

		for (Transaction transaction : periodTransactions) {
			boolean isOutgoing = isOutgoing(transaction, accountNumber);
			BigDecimal amount = safeAmount(transaction.getAmount());

			BigDecimal payments = null;
			BigDecimal receipts = null;
			if (isOutgoing) {
				payments = amount;
				totalWithdrawals = totalWithdrawals.add(amount);
				withdrawalCount += 1;
				runningBalance = runningBalance.subtract(amount);
			} else {
				receipts = amount;
				totalDeposits = totalDeposits.add(amount);
				depositCount += 1;
				runningBalance = runningBalance.add(amount);
			}

			String rowDate = transaction.getTransactionDate() == null
				? range.toDate().format(TABLE_DATE_FORMATTER)
				: transaction.getTransactionDate().format(TABLE_DATE_FORMATTER);
			rows.add(new StatementRow(rowDate, buildParticulars(transaction, isOutgoing), payments, receipts, runningBalance));
		}

		rows.add(new StatementRow(range.toDate().format(TABLE_DATE_FORMATTER), "BALANCE C/F", null, null, runningBalance));

		return new StatementComputation(rows, totalDeposits, totalWithdrawals, depositCount, withdrawalCount);
	}

	// Reverses the effect of one transaction when backtracking opening balance.
	private BigDecimal reverseTransactionEffect(BigDecimal balanceAfterTransaction, Transaction transaction, String accountNumber) {
		BigDecimal amount = safeAmount(transaction.getAmount());
		if (isOutgoing(transaction, accountNumber)) {
			return balanceAfterTransaction.add(amount);
		}
		return balanceAfterTransaction.subtract(amount);
	}

	// Determines whether transaction direction is outgoing relative to account.
	private boolean isOutgoing(Transaction transaction, String accountNumber) {
		return normalizeAccountNumber(transaction.getSenderAccountNo()).equals(accountNumber);
	}

	// Builds statement particulars text line for one transaction row.
	private String buildParticulars(Transaction transaction, boolean isOutgoing) {
		String referenceNo = safeText(transaction.getReferenceNo());
		String remark = safeText(transaction.getRemark());
		String particulars;
		if (isOutgoing) {
			String receiverName = safeText(transaction.getReceiverName());
			String receiverAccountNo = normalizeAccountNumber(transaction.getReceiverAccountNo());
			particulars = receiverName.isBlank()
				? "Transfer to " + receiverAccountNo
				: "Transfer to " + receiverName + " (" + receiverAccountNo + ")";
		} else {
			String senderAccountNo = normalizeAccountNumber(transaction.getSenderAccountNo());
			particulars = "Transfer from " + senderAccountNo;
		}

		if (!remark.isBlank()) {
			particulars = particulars + " - " + remark;
		}
		if (!referenceNo.isBlank()) {
			particulars = particulars + " [" + referenceNo + "]";
		}
		return trimToLength(particulars, 88);
	}

	// Trims long text with ellipsis to keep table layout stable.
	private String trimToLength(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value == null ? "" : value;
		}
		return value.substring(0, maxLength - 3) + "...";
	}

	// Renders complete PDF document from computed statement data.
	private byte[] buildPdf(
		BankCustomer bankCustomer,
		Account account,
		DateRange range,
		StatementComputation computation
	) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			Document document = new Document(PageSize.A4, 36f, 36f, 34f, 70f);
			PdfWriter writer = PdfWriter.getInstance(document, out);
			writer.setPageEvent(new StatementFooterPageEvent());
			document.open();

			addHeaderSection(document, range);
			addCustomerSection(document, bankCustomer, account);
			addStatementTable(document, computation.rows());
			addTotalsSection(document, computation);
			addClosingSection(document, bankCustomer);

			document.close();
			return out.toByteArray();
		} catch (DocumentException ex) {
			throw new IllegalStateException("Failed to generate transaction statement PDF.", ex);
		}
	}

	// Adds top banner/title/date section to the statement document.
	private void addHeaderSection(Document document, DateRange range) throws DocumentException {
		PdfPTable bannerTable = new PdfPTable(1);
		bannerTable.setWidthPercentage(100f);
		PdfPCell bannerCell = new PdfPCell(new Phrase("PRIMECORE DIGITAL BANK", FONT_TITLE_WHITE));
		bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		bannerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		bannerCell.setBackgroundColor(BRAND_PRIMARY);
		bannerCell.setBorder(Rectangle.NO_BORDER);
		bannerCell.setPadding(8f);
		bannerTable.addCell(bannerCell);
		document.add(bannerTable);
		document.add(new Paragraph(" "));

		PdfPTable table = new PdfPTable(new float[] { 4f, 2f });
		table.setWidthPercentage(100f);

		PdfPCell leftCell = new PdfPCell();
		leftCell.setBorder(Rectangle.NO_BORDER);
		leftCell.addElement(new Paragraph("STATEMENT OF ACCOUNT", FONT_TITLE));
		leftCell.addElement(new Paragraph("Transaction Statement", FONT_SMALL));
		table.addCell(leftCell);

		PdfPCell rightCell = new PdfPCell();
		rightCell.setBorder(Rectangle.NO_BORDER);
		rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		rightCell.addElement(new Paragraph("DATE: " + range.toDate().format(STATEMENT_DATE_FORMATTER), FONT_SMALL_BOLD));
		table.addCell(rightCell);

		document.add(table);
		document.add(new Paragraph(" "));
	}

	// Adds customer and account details section.
	private void addCustomerSection(Document document, BankCustomer bankCustomer, Account account) throws DocumentException {
		User user = bankCustomer.getUser();
		String fullName = resolveDisplayName(user).toUpperCase(Locale.ENGLISH);
		String address = safeText(user == null ? null : user.getAddress());
		PdfPTable infoTable = new PdfPTable(new float[] { 2.2f, 3.8f });
		infoTable.setWidthPercentage(100f);

		PdfPCell customerCell = sectionCell();
		customerCell.addElement(new Paragraph("CUSTOMER DETAILS", FONT_SMALL_BOLD));
		customerCell.addElement(new Paragraph(fullName.isBlank() ? "BANK CUSTOMER" : fullName, FONT_SMALL_BOLD));
		if (!address.isBlank()) {
			for (String addressLine : splitAddressLines(address)) {
				customerCell.addElement(new Paragraph(addressLine.toUpperCase(Locale.ENGLISH), FONT_SMALL));
			}
		}
		infoTable.addCell(customerCell);

		PdfPCell accountCell = sectionCell();
		accountCell.addElement(new Paragraph("ACCOUNT DETAILS", FONT_SMALL_BOLD));

		PdfPTable details = new PdfPTable(new float[] { 1.8f, 0.2f, 3f });
		details.setWidthPercentage(100f);
		addDetailRow(details, "ACCOUNT NO", normalizeAccountNumber(account.getAccountNumber()));

		String branchCode = bankCustomer.getBranch() == null ? "" : safeText(bankCustomer.getBranch().getBranchCode());
		String branchName = bankCustomer.getBranch() == null ? "" : safeText(bankCustomer.getBranch().getBranchName());
		addDetailRow(details, "BRANCH", (branchCode + " " + branchName).trim());
		addDetailRow(details, "CURRENCY", "LKR");
		addDetailRow(details, "ACCOUNT TYPE", safeText(account.getAccountType()).toUpperCase(Locale.ENGLISH));

		accountCell.addElement(details);
		infoTable.addCell(accountCell);

		document.add(infoTable);
		document.add(new Paragraph(" "));
	}

	// Adds main statement transaction table with alternating row styles.
	private void addStatementTable(Document document, List<StatementRow> rows) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[] { 1.2f, 4.5f, 2f, 2f, 2f });
		table.setWidthPercentage(100f);

		table.addCell(headerCell("DATE"));
		table.addCell(headerCell("PARTICULARS"));
		table.addCell(headerCell("PAYMENTS"));
		table.addCell(headerCell("RECEIPTS"));
		table.addCell(headerCell("BALANCE"));

		for (int i = 0; i < rows.size(); i += 1) {
			StatementRow row = rows.get(i);
			Color rowColor = i % 2 == 0 ? ROW_BG : ROW_BG_ALT;

			table.addCell(bodyCell(row.date(), Element.ALIGN_LEFT, rowColor));
			table.addCell(bodyCell(row.particulars(), Element.ALIGN_LEFT, rowColor));
			table.addCell(bodyCell(formatAmountOrBlank(row.payments()), Element.ALIGN_RIGHT, rowColor));
			table.addCell(bodyCell(formatAmountOrBlank(row.receipts()), Element.ALIGN_RIGHT, rowColor));
			table.addCell(bodyCell(formatAmount(row.balance()), Element.ALIGN_RIGHT, rowColor));
		}

		document.add(table);
		document.add(new Paragraph("  "));
	}

	// Adds totals block for unrealized cheques, deposits, and withdrawals.
	private void addTotalsSection(Document document, StatementComputation computation) throws DocumentException {
		PdfPTable totals = new PdfPTable(new float[] { 3.3f, 1.4f, 1.6f });
		totals.setWidthPercentage(57f);
		totals.setHorizontalAlignment(Element.ALIGN_LEFT);

		totals.addCell(totalLabelCell("TOTAL UNREALIZED CHEQUES"));
		totals.addCell(totalValueCell(""));
		totals.addCell(totalValueCell(formatAmount(BigDecimal.ZERO)));

		totals.addCell(totalLabelCell("TOTAL DEPOSITS"));
		totals.addCell(totalValueCell(computation.depositCount() + " item(s)"));
		totals.addCell(totalValueCell(formatAmount(computation.totalDeposits())));

		totals.addCell(totalLabelCell("TOTAL WITHDRAWALS"));
		totals.addCell(totalValueCell(computation.withdrawalCount() + " item(s)"));
		totals.addCell(totalValueCell(formatAmount(computation.totalWithdrawals())));

		document.add(totals);
		document.add(new Paragraph("  "));
	}

	// Adds closing message and contact hint section.
	private void addClosingSection(Document document, BankCustomer bankCustomer) throws DocumentException {
		String branchPhone = bankCustomer.getBranch() == null ? "" : safeText(bankCustomer.getBranch().getBranchPhone());
		String message = branchPhone.isBlank()
			? "For clarifications, if any, please contact the Branch Manager."
			: "For clarifications, if any, please contact the Branch Manager on " + branchPhone;

		Paragraph end = centeredParagraph("END OF STATEMENT", FONT_SUBTITLE);
		document.add(end);
		document.add(centeredParagraph(message, FONT_SMALL));
	}

	// Creates a horizontally centered paragraph with a specific font.
	private Paragraph centeredParagraph(String value, Font font) {
		Paragraph paragraph = new Paragraph(value, font);
		paragraph.setAlignment(Element.ALIGN_CENTER);
		return paragraph;
	}

	// Builds shared section container style cell.
	private PdfPCell sectionCell() {
		PdfPCell cell = new PdfPCell();
		cell.setBorderColor(SECTION_BORDER);
		cell.setBorderWidth(1f);
		cell.setBackgroundColor(SECTION_BG);
		cell.setPadding(8f);
		return cell;
	}

	// Adds one key/value detail row into account-details mini-table.
	private void addDetailRow(PdfPTable table, String label, String value) {
		PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_SMALL_BOLD));
		labelCell.setBorder(Rectangle.NO_BORDER);
		labelCell.setPaddingTop(2f);
		labelCell.setPaddingBottom(2f);

		PdfPCell colonCell = new PdfPCell(new Phrase(":", FONT_SMALL_BOLD));
		colonCell.setBorder(Rectangle.NO_BORDER);
		colonCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		colonCell.setPaddingTop(2f);
		colonCell.setPaddingBottom(2f);

		PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_SMALL_BOLD));
		valueCell.setBorder(Rectangle.NO_BORDER);
		valueCell.setPaddingTop(2f);
		valueCell.setPaddingBottom(2f);

		table.addCell(labelCell);
		table.addCell(colonCell);
		table.addCell(valueCell);
	}

	// Builds styled header cell for statement table.
	private PdfPCell headerCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_TABLE_HEAD));
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setBackgroundColor(value == null || value.isBlank() ? BRAND_ACCENT : BRAND_PRIMARY);
		cell.setBorderColor(WHITE_GRID);
		cell.setBorderWidth(1.5f);
		cell.setPadding(5f);
		return cell;
	}

	// Builds styled body cell for statement table.
	private PdfPCell bodyCell(String value, int horizontalAlignment, Color backgroundColor) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_TABLE_BODY));
		cell.setHorizontalAlignment(horizontalAlignment);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setBackgroundColor(backgroundColor);
		cell.setBorderColor(WHITE_GRID);
		cell.setBorderWidth(1.3f);
		cell.setPadding(4f);
		return cell;
	}

	// Builds styled label cell used in totals section.
	private PdfPCell totalLabelCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_SMALL_BOLD_WHITE));
		cell.setBackgroundColor(BRAND_SECONDARY);
		cell.setBorderColor(WHITE_GRID);
		cell.setBorderWidth(1.3f);
		cell.setPadding(4f);
		return cell;
	}

	// Builds styled value cell used in totals section.
	private PdfPCell totalValueCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_SMALL));
		cell.setBackgroundColor(TOTAL_VALUE_BG);
		cell.setBorderColor(WHITE_GRID);
		cell.setBorderWidth(1.3f);
		cell.setPadding(4f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		return cell;
	}

	// Returns formatted amount string or blank when value is null.
	private String formatAmountOrBlank(BigDecimal value) {
		if (value == null) {
			return "";
		}
		return formatAmount(value);
	}

	// Formats numeric amount with 2 decimal places and grouping separators.
	private String formatAmount(BigDecimal value) {
		DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		return decimalFormat.format(safeAmount(value));
	}

	// Splits address text into clean printable lines.
	private List<String> splitAddressLines(String address) {
		String sanitized = address.replace("\r", "").trim();
		if (sanitized.isBlank()) {
			return List.of();
		}

		String[] parts = sanitized.split(",\\s*");
		List<String> lines = new ArrayList<>();
		for (String part : parts) {
			String line = part.trim();
			if (!line.isBlank()) {
				lines.add(line);
			}
		}
		if (lines.isEmpty()) {
			lines.add(sanitized);
		}
		return lines;
	}

	// Normalizes account number by trimming and removing internal spaces.
	private String normalizeAccountNumber(String accountNo) {
		return accountNo == null ? "" : accountNo.replaceAll("\\s+", "").trim();
	}

	// Returns zero when amount is null.
	private BigDecimal safeAmount(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO : amount;
	}

	// Returns trimmed text or empty fallback for null values.
	private String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	// Resolves best-available display name for statement customer block.
	private String resolveDisplayName(User user) {
		if (user == null) {
			return "";
		}
		String firstName = safeText(user.getFirstName());
		String lastName = safeText(user.getLastName());
		String fullName = (firstName + " " + lastName).trim();
		if (!fullName.isBlank()) {
			return fullName;
		}
		String username = safeText(user.getUsername());
		if (!username.isBlank()) {
			return username;
		}
		return safeText(user.getEmail());
	}

	// Resolves account linked to authenticated bank customer.
	private Account resolveOwnedAccountForBankCustomer(BankCustomer bankCustomer) {
		if (bankCustomer == null || bankCustomer.getAccount() == null || bankCustomer.getAccount().getAccountId() == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found for logged-in bank customer.");
		}
		return accountRepository
			.findById(bankCustomer.getAccount().getAccountId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found for logged-in bank customer."));
	}

	// Resolves and validates authenticated principal as BANK_CUSTOMER.
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

	// Result wrapper for generated statement file name and binary content.
	public record StatementPdfResult(String fileName, byte[] content) {}

	// Inclusive statement date range.
	private record DateRange(LocalDate fromDate, LocalDate toDate) {}

	// One printable row in statement transaction table.
	private record StatementRow(
		String date,
		String particulars,
		BigDecimal payments,
		BigDecimal receipts,
		BigDecimal balance
	) {}

	// Computed statement rows plus totals for summary section.
	private record StatementComputation(
		List<StatementRow> rows,
		BigDecimal totalDeposits,
		BigDecimal totalWithdrawals,
		int depositCount,
		int withdrawalCount
	) {}

	// Custom PDF footer renderer (page number, marker, and disclaimer).
	private static class StatementFooterPageEvent extends PdfPageEventHelper {

		@Override
		// Draws footer lines at the end of each PDF page.
		public void onEndPage(PdfWriter writer, Document document) {
			ColumnText.showTextAligned(
				writer.getDirectContent(),
				Element.ALIGN_LEFT,
				new Phrase("page no: " + writer.getPageNumber(), FONT_FOOTER_ITALIC),
				document.left(),
				document.bottom() - 18f,
				0f
			);
			ColumnText.showTextAligned(
				writer.getDirectContent(),
				Element.ALIGN_RIGHT,
				new Phrase("PRIMECORE-IT", FONT_FOOTER_ITALIC),
				document.right(),
				document.bottom() - 18f,
				0f
			);
			ColumnText.showTextAligned(
				writer.getDirectContent(),
				Element.ALIGN_LEFT,
				new Phrase(
					"All transactions shown above are based on verified records. Queries beyond 3 months may incur a service fee.",
					FONT_FOOTER
				),
				document.left(),
				document.bottom() - 30f,
				0f
			);
		}
	}
}
