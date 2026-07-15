package com.mvp.user.service;

import com.mvp.common.dto.auth.AuthTokenDTO;

/**
 * Token Service。
 *
 * <p>只负责独立 token 能力。用户注册、登录、登出和当前用户查询统一放在 UserService 中。</p>
 */
public interface AuthService {

    /**
     * 使用 refreshToken 刷新 accessToken。
     *
     * <p>refreshToken 需要同时通过 JWT 校验和 Redis 白名单校验。
     * 当前 refreshToken 使用固定会话有效期，刷新 accessToken 时不续期。</p>
     *
     * @param refreshToken refreshToken
     * @return 新的 accessToken 和对应过期时间
     */
    AuthTokenDTO refresh(String refreshToken);
}
