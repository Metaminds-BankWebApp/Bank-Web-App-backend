package com.bank_web_app.backend.user.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts the birth-date portion of supported Sri Lankan NIC formats to a date. */
public final class SriLankanNicDateOfBirthResolver {

	private static final Pattern OLD_NIC_PATTERN = Pattern.compile("^(\\d{2})(\\d{3})\\d{4}[Vv]$");
	private static final Pattern NEW_NIC_PATTERN = Pattern.compile("^(\\d{4})(\\d{3})\\d{5}$");

	private SriLankanNicDateOfBirthResolver() {
	}

	public static LocalDate resolve(String nic) {
		String normalizedNic = nic == null ? "" : nic.trim();
		Matcher oldNic = OLD_NIC_PATTERN.matcher(normalizedNic);
		Matcher newNic = NEW_NIC_PATTERN.matcher(normalizedNic);

		int year;
		int encodedDayOfYear;
		if (oldNic.matches()) {
			year = 1900 + Integer.parseInt(oldNic.group(1));
			encodedDayOfYear = Integer.parseInt(oldNic.group(2));
		} else if (newNic.matches()) {
			year = Integer.parseInt(newNic.group(1));
			encodedDayOfYear = Integer.parseInt(newNic.group(2));
		} else {
			throw new IllegalArgumentException("Enter a valid Sri Lankan NIC number.");
		}

		int dayOfYear = encodedDayOfYear > 500 ? encodedDayOfYear - 500 : encodedDayOfYear;
		try {
			LocalDate dateOfBirth = LocalDate.ofYearDay(year, dayOfYear);
			if (dateOfBirth.isAfter(LocalDate.now())) {
				throw new IllegalArgumentException("NIC contains a future date of birth.");
			}
			return dateOfBirth;
		} catch (DateTimeException ex) {
			throw new IllegalArgumentException("NIC contains an invalid date of birth.");
		}
	}
}
