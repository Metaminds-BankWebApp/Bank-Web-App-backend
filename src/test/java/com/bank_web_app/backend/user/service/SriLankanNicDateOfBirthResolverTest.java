package com.bank_web_app.backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SriLankanNicDateOfBirthResolverTest {

	@Test
	void resolvesOldFormatNicDateOfBirth() {
		assertThat(SriLankanNicDateOfBirthResolver.resolve("900010001V"))
			.isEqualTo(LocalDate.of(1990, 1, 1));
	}

	@Test
	void resolvesNewFormatFemaleNicDateOfBirth() {
		assertThat(SriLankanNicDateOfBirthResolver.resolve("200050100001"))
			.isEqualTo(LocalDate.of(2000, 1, 1));
	}

	@Test
	void rejectsNicWithAnInvalidDayOfYear() {
		assertThatThrownBy(() -> SriLankanNicDateOfBirthResolver.resolve("201936600001"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("NIC contains an invalid date of birth.");
	}
}
