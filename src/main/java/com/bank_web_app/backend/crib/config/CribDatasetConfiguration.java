package com.bank_web_app.backend.crib.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class CribDatasetConfiguration {

	@Bean(name = "cribJdbcTemplate")
	public JdbcTemplate cribJdbcTemplate(CribDatasetProperties properties) {
		return new JdbcTemplate(createCribDataSource(properties));
	}

	private DriverManagerDataSource createCribDataSource(CribDatasetProperties properties) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(properties.getDriverClassName());
		dataSource.setUrl(properties.getUrl() == null ? "" : properties.getUrl());
		dataSource.setUsername(properties.getUsername() == null ? "" : properties.getUsername());
		dataSource.setPassword(properties.getPassword() == null ? "" : properties.getPassword());
		return dataSource;
	}
}
