package com.mvp.user.service.impl;

import com.mvp.common.dto.auth.AuthTokenDTO;
import com.mvp.common.jwt.JwtPayload;
import com.mvp.common.jwt.JwtUtil;
import com.mvp.user.config.AuthProperties;
import com.mvp.user.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Token Service 实现。
 *
 * <p>只保留独立 token 能力：使用 refreshToken 刷新 accessToken。</p>
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final RedissonClient redissonClient;
    private final AuthProperties authProperties;

    public AuthServiceImpl(JwtUtil jwtUtil,
                           RedissonClient redissonClient,
                           AuthProperties authProperties) {
        this.jwtUtil = jwtUtil;
        this.redissonClient = redissonClient;
        this.authProperties = authProperties;
    }

    /**
     * 使用 refreshToken 刷新 accessToken。
     *
     * <p>refreshToken 有固定会话有效期：登录时签发后最多存活 refresh-token-seconds 秒。
     * 刷新 accessToken 时不重新签发 refreshToken，避免用户只要持续请求就无限滑动续期。</p>
     */
    @Override
    public AuthTokenDTO refresh(String refreshToken) {
        JwtPayload refreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);

        String refreshKey = refreshKey(refreshPayload.getSub(), refreshPayload.getJti());
        if (!redissonClient.getBucket(refreshKey).isExists()) {
            throw new IllegalArgumentException("refreshToken 已失效");
        }

        long refreshRemainSeconds = refreshPayload.getExp() - Instant.now().getEpochSecond();
        long accessTokenSeconds = Math.min(jwtUtil.getAccessTokenSeconds(), refreshRemainSeconds);
        if (accessTokenSeconds <= 0) {
            throw new IllegalArgumentException("refreshToken 已过期");
        }

        String accessToken = jwtUtil.createAccessToken(refreshPayload.getSub(), accessTokenSeconds);
        AuthTokenDTO tokenDTO = toAuthTokenDTO(accessToken, refreshToken);
        tokenDTO.setAccessTokenExpiresIn(accessTokenSeconds);
        tokenDTO.setRefreshTokenExpiresIn(refreshRemainSeconds);
        log.info("刷新 accessToken 成功 userId={} refreshRemainSeconds={}", refreshPayload.getSub(), refreshRemainSeconds);
        return tokenDTO;
    }

    /**
     * 组装 token 响应体。
     */
    private AuthTokenDTO toAuthTokenDTO(String accessToken, String refreshToken) {
        AuthTokenDTO tokenDTO = new AuthTokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setAccessTokenExpiresIn(jwtUtil.getAccessTokenSeconds());
        tokenDTO.setRefreshTokenExpiresIn(jwtUtil.getRefreshTokenSeconds());
        return tokenDTO;
    }

    /**
     * 生成 refreshToken Redis 白名单 key。
     *
     * <p>key 格式：auth:refresh:{userId}:{refreshJti}</p>
     */
    private String refreshKey(String userId, String refreshJti) {
        return authProperties.getRefreshKeyPrefix() + userId + ":" + refreshJti;
    }
}
