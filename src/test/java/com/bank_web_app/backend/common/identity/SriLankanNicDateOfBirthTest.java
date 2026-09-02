package com.bank_web_app.backend.common.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SriLankanNicDateOfBirthTest {

	@Test
	void derivesBirthDatesFromNewAndOldNicFormats() {
		assertThat(SriLankanNicDateOfBirth.parse("198201409894"))
			.contains(LocalDate.of(1982, 1, 14));
		assertThat(SriLankanNicDateOfBirth.parse("855420159v"))
			.contains(LocalDate.of(1985, 2, 11));
		assertThat(SriLankanNicDateOfBirth.parse("850420159X"))
			.contains(LocalDate.of(1985, 2, 11));
	}

	@Test
	void usesTheNicFixedLeapDayCalendar() {
		assertThat(SriLankanNicDateOfBirth.parse("198506109894"))
			.contains(LocalDate.of(1985, 3, 1));
		assertThat(SriLankanNicDateOfBirth.parse("198506009894")).isEmpty();
		assertThat(SriLankanNicDateOfBirth.parse("198406009894"))
			.contains(LocalDate.of(1984, 2, 29));
	}

	@Test
	void rejectsInvalidNicFormatsAndDayCodes() {
		assertThat(SriLankanNicDateOfBirth.parse("198236709894")).isEmpty();
		assertThat(SriLankanNicDateOfBirth.parse("198250009894")).isEmpty();
		assertThat(SriLankanNicDateOfBirth.parse("not-a-nic")).isEmpty();
	}
}
