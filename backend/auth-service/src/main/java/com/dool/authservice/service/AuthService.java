package com.dool.authservice.service;

import com.dool.authservice.request.LoginRequest;
import com.dool.authservice.response.TokenResponse;

import javax.servlet.http.HttpServletResponse;
import java.net.http.HttpResponse;

public interface AuthService {
    public void userLogin(LoginRequest request, HttpServletResponse httpServletResponse);


}
