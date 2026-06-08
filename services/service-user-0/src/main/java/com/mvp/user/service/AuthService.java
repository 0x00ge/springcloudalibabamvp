package com.mvp.user.service;

import com.mvp.model.dto.auth.AuthTokenDTO;
import com.mvp.model.dto.auth.CurrentAuthDTO;

/**
 * 用户鉴权 Service。
 */
public interface AuthService {

    CurrentAuthDTO register(String phone, String name, String password, String confirmPassword);

    AuthTokenDTO login(String phone, String password);

    AuthTokenDTO refresh(String refreshToken);

    void logout(String authorization, String refreshToken);

    CurrentAuthDTO currentUser(String userId);
}
