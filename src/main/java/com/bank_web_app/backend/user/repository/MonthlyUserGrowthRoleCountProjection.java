package com.bank_web_app.backend.user.repository;

public interface MonthlyUserGrowthRoleCountProjection {

	Integer getYearValue();

	Integer getMonthValue();

	String getRoleName();

	Long getUserCount();
}
