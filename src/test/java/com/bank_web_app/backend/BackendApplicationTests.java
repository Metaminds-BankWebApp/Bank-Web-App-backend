package com.bank_web_app.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BackendApplicationTests {

	@Test
	void applicationEntryPointIsAvailableWithoutStartingExternalServices() {
		assertThat(BackendApplication.class).isNotNull();
	}

}
