package com.bank_web_app.backend.auth.service;

import com.bank_web_app.backend.auth.dto.request.LoginRequest;
import com.bank_web_app.backend.auth.dto.request.RefreshTokenRequest;
import com.bank_web_app.backend.auth.dto.response.LoginResponse;

public interface AuthService {

	LoginResponse login(LoginRequest request);

	LoginResponse refresh(RefreshTokenRequest request);

	void logout(RefreshTokenRequest request);
}
