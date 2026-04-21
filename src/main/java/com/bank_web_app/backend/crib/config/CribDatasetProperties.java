package com.bank_web_app.backend.crib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "crib.datasource")
public class CribDatasetProperties {

	private String url;
	private String username;
	private String password;
	private String driverClassName = "org.postgresql.Driver";
	private String schema = "public";

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		if (driverClassName != null && !driverClassName.isBlank()) {
			this.driverClassName = driverClassName;
		}
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		if (schema != null && !schema.isBlank()) {
			this.schema = schema;
		}
	}

	public boolean isConfigured() {
		return url != null && !url.isBlank();
	}
}