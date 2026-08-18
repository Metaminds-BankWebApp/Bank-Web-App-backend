package com.bank_web_app.backend.auth.service;

import com.bank_web_app.backend.auth.dto.request.ForgotPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.LoginRequest;
import com.bank_web_app.backend.auth.dto.request.RefreshTokenRequest;
import com.bank_web_app.backend.auth.dto.request.ResetPasswordRequest;
import com.bank_web_app.backend.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.bank_web_app.backend.auth.dto.response.AuthActionResponse;
import com.bank_web_app.backend.auth.dto.response.AuthMeResponse;
import com.bank_web_app.backend.auth.dto.response.LoginResponse;

public interface AuthService {

	LoginResponse login(LoginRequest request);

	LoginResponse refresh(RefreshTokenRequest request);

	void logout(RefreshTokenRequest request);

	AuthActionResponse forgotPassword(ForgotPasswordRequest request);

	AuthActionResponse verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request);

	AuthActionResponse resetPassword(ResetPasswordRequest request);

	AuthMeResponse me();
}
