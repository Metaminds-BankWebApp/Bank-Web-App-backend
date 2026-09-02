package com.bank_web_app.backend.common.identity;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Derives the date of birth encoded in old and new Sri Lankan NIC numbers. */
public final class SriLankanNicDateOfBirth {

	private static final Pattern OLD_NIC_PATTERN = Pattern.compile("^\\d{9}[VX]$", Pattern.CASE_INSENSITIVE);
	private static final Pattern NEW_NIC_PATTERN = Pattern.compile("^\\d{12}$");
	private static final int[] FIXED_NIC_MONTH_LENGTHS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

	private SriLankanNicDateOfBirth() {}

	public static Optional<LocalDate> parse(String nic) {
		String normalizedNic = nic == null ? "" : nic.trim().toUpperCase(Locale.ROOT);
		boolean oldFormat = OLD_NIC_PATTERN.matcher(normalizedNic).matches();
		if (!oldFormat && !NEW_NIC_PATTERN.matcher(normalizedNic).matches()) {
			return Optional.empty();
		}

		int year = oldFormat
			? 1900 + Integer.parseInt(normalizedNic.substring(0, 2))
			: Integer.parseInt(normalizedNic.substring(0, 4));
		int encodedDay = Integer.parseInt(
			oldFormat ? normalizedNic.substring(2, 5) : normalizedNic.substring(4, 7)
		);
		int dayOfFixedYear = encodedDay > 500 ? encodedDay - 500 : encodedDay;
		if (year < 1900 || dayOfFixedYear < 1 || dayOfFixedYear > 366) {
			return Optional.empty();
		}

		int month = 1;
		int dayOfMonth = dayOfFixedYear;
		for (int monthLength : FIXED_NIC_MONTH_LENGTHS) {
			if (dayOfMonth <= monthLength) {
				break;
			}
			dayOfMonth -= monthLength;
			month++;
		}

		try {
			return Optional.of(LocalDate.of(year, month, dayOfMonth));
		} catch (DateTimeException exception) {
			return Optional.empty();
		}
	}
}
