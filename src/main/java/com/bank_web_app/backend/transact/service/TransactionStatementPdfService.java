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
	private static final Color TOTAL_VALUE_BG = new Color(244, 250, 255);
	private static final Color WHITE_GRID = new Color(255, 255, 255);

	private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 10);
	private static final Font FONT_SMALL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
	private static final Font FONT_SMALL_BOLD_WHITE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
	private static final Font FONT_TABLE_HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
	private static final Font FONT_TABLE_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9);
	private static final Font FONT_LOGO_MARK = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.WHITE);
	private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
	private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
	private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA, 8);
	private static final Font FONT_FOOTER_ITALIC = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

	private final TransactionRepository transactionRepository;
	private final BankCustomerRepository bankCustomerRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;

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

	private BigDecimal reverseTransactionEffect(BigDecimal balanceAfterTransaction, Transaction transaction, String accountNumber) {
		BigDecimal amount = safeAmount(transaction.getAmount());
		if (isOutgoing(transaction, accountNumber)) {
			return balanceAfterTransaction.add(amount);
		}
		return balanceAfterTransaction.subtract(amount);
	}

	private boolean isOutgoing(Transaction transaction, String accountNumber) {
		return normalizeAccountNumber(transaction.getSenderAccountNo()).equals(accountNumber);
	}

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

	private String trimToLength(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value == null ? "" : value;
		}
		return value.substring(0, maxLength - 3) + "...";
	}

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

	private void addHeaderSection(Document document, DateRange range) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[] { 2f, 3f, 2f });
		table.setWidthPercentage(100f);

		PdfPCell leftCell = new PdfPCell(buildBankBadgeTable());
		leftCell.setBorder(Rectangle.NO_BORDER);
		table.addCell(leftCell);

		PdfPCell centerCell = new PdfPCell();
		centerCell.setBorder(Rectangle.NO_BORDER);
		centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		centerCell.addElement(centeredParagraph("STATEMENT OF ACCOUNT", FONT_TITLE));
			centerCell.addElement(centeredParagraph("PRIMECORE DIGITAL BANK", FONT_SUBTITLE));
			table.addCell(centerCell);

		PdfPCell rightCell = new PdfPCell();
		rightCell.setBorder(Rectangle.NO_BORDER);
		rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		rightCell.addElement(new Paragraph("DATE: " + range.toDate().format(STATEMENT_DATE_FORMATTER), FONT_SMALL_BOLD));
		table.addCell(rightCell);

		document.add(table);
		document.add(new Paragraph("  "));
	}

	private PdfPTable buildBankBadgeTable() {
		PdfPTable badgeTable = new PdfPTable(1);
		badgeTable.setWidthPercentage(65f);

		PdfPCell bankCell = new PdfPCell();
		bankCell.setBorder(Rectangle.NO_BORDER);
		bankCell.addElement(new Paragraph("PRIMECORE BANK", FONT_SMALL_BOLD));

		PdfPTable logoTable = new PdfPTable(1);
		logoTable.setWidthPercentage(100f);
		PdfPCell logoCell = new PdfPCell();
		logoCell.setBackgroundColor(BRAND_PRIMARY);
		logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		logoCell.setBorderColor(WHITE_GRID);
		logoCell.setBorderWidth(1.2f);
		logoCell.setPaddingTop(3f);
		logoCell.setPaddingBottom(2f);
		logoCell.addElement(centeredParagraph("PC", FONT_LOGO_MARK));
		logoTable.addCell(logoCell);

		bankCell.addElement(logoTable);
		bankCell.addElement(new Paragraph("Digital Banking for Sri Lanka", FONT_SMALL));
		badgeTable.addCell(bankCell);
		return badgeTable;
	}

	private void addCustomerSection(Document document, BankCustomer bankCustomer, Account account) throws DocumentException {
		User user = bankCustomer.getUser();
		String fullName = resolveDisplayName(user).toUpperCase(Locale.ENGLISH);
		String address = safeText(user == null ? null : user.getAddress());

		Paragraph nameLine = new Paragraph(fullName.isBlank() ? "BANK CUSTOMER" : fullName, FONT_SMALL_BOLD);
		document.add(nameLine);
		if (!address.isBlank()) {
			for (String addressLine : splitAddressLines(address)) {
				document.add(new Paragraph(addressLine.toUpperCase(Locale.ENGLISH), FONT_SMALL));
			}
		}
		document.add(new Paragraph("  "));

		PdfPTable details = new PdfPTable(new float[] { 2f, 5f });
		details.setWidthPercentage(72f);
		details.addCell(noBorderDetailCell("ACCOUNT NO"));
		details.addCell(noBorderDetailCell(": " + normalizeAccountNumber(account.getAccountNumber())));

		String branchCode = bankCustomer.getBranch() == null ? "" : safeText(bankCustomer.getBranch().getBranchCode());
		String branchName = bankCustomer.getBranch() == null ? "" : safeText(bankCustomer.getBranch().getBranchName());
		details.addCell(noBorderDetailCell("BRANCH CODE/NAME"));
		details.addCell(noBorderDetailCell(": " + (branchCode + " " + branchName).trim()));

		details.addCell(noBorderDetailCell("CURRENCY"));
		details.addCell(noBorderDetailCell(": LKR"));
		details.addCell(noBorderDetailCell("ACCOUNT TYPE"));
		details.addCell(noBorderDetailCell(": " + safeText(account.getAccountType()).toUpperCase(Locale.ENGLISH)));

		document.add(details);
		document.add(new Paragraph(" "));
	}

	private void addStatementTable(Document document, List<StatementRow> rows) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[] { 1.2f, 4.35f, 2f, 2f, 2f, 0.35f });
		table.setWidthPercentage(100f);

		table.addCell(headerCell("DATE"));
		table.addCell(headerCell("PARTICULARS"));
		table.addCell(headerCell("PAYMENTS"));
		table.addCell(headerCell("RECEIPTS"));
		table.addCell(headerCell("BALANCE"));
		table.addCell(headerCell(""));

		for (StatementRow row : rows) {
			table.addCell(bodyCell(row.date(), Element.ALIGN_LEFT));
			table.addCell(bodyCell(row.particulars(), Element.ALIGN_LEFT));
			table.addCell(bodyCell(formatAmountOrBlank(row.payments()), Element.ALIGN_RIGHT));
			table.addCell(bodyCell(formatAmountOrBlank(row.receipts()), Element.ALIGN_RIGHT));
			table.addCell(bodyCell(formatAmount(row.balance()), Element.ALIGN_RIGHT));
			table.addCell(bodyCell("", Element.ALIGN_LEFT));
		}

		document.add(table);
		document.add(new Paragraph("  "));
	}

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

	private void addClosingSection(Document document, BankCustomer bankCustomer) throws DocumentException {
		String branchPhone = bankCustomer.getBranch() == null ? "" : safeText(bankCustomer.getBranch().getBranchPhone());
		String message = branchPhone.isBlank()
			? "For clarifications, if any, please contact the Branch Manager."
			: "For clarifications, if any, please contact the Branch Manager on " + branchPhone;

		Paragraph end = centeredParagraph("END OF STATEMENT", FONT_SUBTITLE);
		document.add(end);
		document.add(centeredParagraph(message, FONT_SMALL));
	}

	private Paragraph centeredParagraph(String value, Font font) {
		Paragraph paragraph = new Paragraph(value, font);
		paragraph.setAlignment(Element.ALIGN_CENTER);
		return paragraph;
	}

	private PdfPCell noBorderDetailCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_SMALL_BOLD));
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setPadding(2f);
		return cell;
	}

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

	private PdfPCell bodyCell(String value, int horizontalAlignment) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_TABLE_BODY));
		cell.setHorizontalAlignment(horizontalAlignment);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setBackgroundColor(ROW_BG);
		cell.setBorderColor(WHITE_GRID);
		cell.setBorderWidth(1.3f);
		cell.setPadding(4f);
		return cell;
	}

	private PdfPCell totalLabelCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_SMALL_BOLD_WHITE));
		cell.setBackgroundColor(BRAND_SECONDARY);
		cell.setBorderColor(WHITE_GRID);
		cell.setBorderWidth(1.3f);
		cell.setPadding(4f);
		return cell;
	}

	private PdfPCell totalValueCell(String value) {
		PdfPCell cell = new PdfPCell(new Phrase(value, FONT_SMALL));
		cell.setBackgroundColor(TOTAL_VALUE_BG);
		cell.setBorderColor(WHITE_GRID);
		cell.setBorderWidth(1.3f);
		cell.setPadding(4f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		return cell;
	}

	private String formatAmountOrBlank(BigDecimal value) {
		if (value == null) {
			return "";
		}
		return formatAmount(value);
	}

	private String formatAmount(BigDecimal value) {
		DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		return decimalFormat.format(safeAmount(value));
	}

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

	private String normalizeAccountNumber(String accountNo) {
		return accountNo == null ? "" : accountNo.replaceAll("\\s+", "").trim();
	}

	private BigDecimal safeAmount(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO : amount;
	}

	private String safeText(String value) {
		return value == null ? "" : value.trim();
	}

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

	private Account resolveOwnedAccountForBankCustomer(BankCustomer bankCustomer) {
		if (bankCustomer == null || bankCustomer.getAccount() == null || bankCustomer.getAccount().getAccountId() == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found for logged-in bank customer.");
		}
		return accountRepository
			.findById(bankCustomer.getAccount().getAccountId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account was not found for logged-in bank customer."));
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

	public record StatementPdfResult(String fileName, byte[] content) {}

	private record DateRange(LocalDate fromDate, LocalDate toDate) {}

	private record StatementRow(
		String date,
		String particulars,
		BigDecimal payments,
		BigDecimal receipts,
		BigDecimal balance
	) {}

	private record StatementComputation(
		List<StatementRow> rows,
		BigDecimal totalDeposits,
		BigDecimal totalWithdrawals,
		int depositCount,
		int withdrawalCount
	) {}

	private static class StatementFooterPageEvent extends PdfPageEventHelper {

		@Override
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
