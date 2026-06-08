package com.mvp.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mvp.common.jwt.JwtPayload;
import com.mvp.common.jwt.JwtUtil;
import com.mvp.model.entity.user.User;
import com.mvp.model.enums.ResultCode;
import com.mvp.model.dto.auth.AuthTokenDTO;
import com.mvp.model.dto.auth.CurrentAuthDTO;
import com.mvp.user.config.AuthProperties;
import com.mvp.user.service.AuthService;
import com.mvp.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * 用户鉴权 Service 实现。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int USER_STATUS_NORMAL = 1;
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserService userService,
                           JwtUtil jwtUtil,
                           StringRedisTemplate stringRedisTemplate,
                           AuthProperties authProperties) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.authProperties = authProperties;
    }

    @Override
    public CurrentAuthDTO register(String phone, String name, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次密码不一致");
        }

        boolean exists = userService.count(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone)) > 0;
        if (exists) {
            throw new IllegalArgumentException(ResultCode.PHONE_EXIST.getMessage());
        }

        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setGender(0);
        user.setStatus(USER_STATUS_NORMAL);
        userService.save(user);
        log.info("用户注册成功 userId={} phone={}", user.getId(), phone);
        return toCurrentAuthDTO(user);
    }

    @Override
    public AuthTokenDTO login(String phone, String password) {
        User user = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone), false);
        if (user == null) {
            throw new IllegalArgumentException(ResultCode.USER_NOT_FOUND.getMessage());
        }
        if (user.getStatus() == null || user.getStatus() != USER_STATUS_NORMAL) {
            throw new IllegalArgumentException(ResultCode.ACCOUNT_DISABLED.getMessage());
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("手机号或密码错误");
        }

        user.setLastLoginAt(new Date());
        userService.updateById(user);
        AuthTokenDTO tokenDTO = createAndSaveToken(user.getId());
        log.info("用户登录成功 userId={} phone={}", user.getId(), phone);
        return tokenDTO;
    }

    @Override
    public AuthTokenDTO refresh(String refreshToken) {
        JwtPayload oldRefreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);
        String refreshKey = refreshKey(oldRefreshPayload.getSub(), oldRefreshPayload.getJti());
        Boolean exists = stringRedisTemplate.hasKey(refreshKey);
        if (!Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException("refreshToken 已失效");
        }

        stringRedisTemplate.delete(refreshKey);
        AuthTokenDTO tokenDTO = createAndSaveToken(oldRefreshPayload.getSub());
        log.info("刷新双 token 成功 userId={}", oldRefreshPayload.getSub());
        return tokenDTO;
    }

    @Override
    public void logout(String authorization, String refreshToken) {
        JwtPayload accessPayload = jwtUtil.parseAndValidate(extractBearerToken(authorization), JwtUtil.TYPE_ACCESS);
        JwtPayload refreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);

        long accessTtlSeconds = Math.max(1, accessPayload.getExp() - Instant.now().getEpochSecond());
        stringRedisTemplate.opsForValue().set(
                authProperties.getBlacklistKeyPrefix() + accessPayload.getJti(),
                accessPayload.getSub(),
                Duration.ofSeconds(accessTtlSeconds)
        );
        stringRedisTemplate.delete(refreshKey(refreshPayload.getSub(), refreshPayload.getJti()));
        log.info("用户登出成功 userId={} accessJti={}", accessPayload.getSub(), accessPayload.getJti());
    }

    @Override
    public CurrentAuthDTO currentUser(String userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException(ResultCode.USER_NOT_FOUND.getMessage());
        }
        return toCurrentAuthDTO(user);
    }

    private AuthTokenDTO createAndSaveToken(String userId) {
        String accessToken = jwtUtil.createAccessToken(userId);
        String refreshToken = jwtUtil.createRefreshToken(userId);
        JwtPayload refreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);

        stringRedisTemplate.opsForValue().set(
                refreshKey(userId, refreshPayload.getJti()),
                userId,
                Duration.ofSeconds(jwtUtil.getRefreshTokenSeconds())
        );

        AuthTokenDTO tokenDTO = new AuthTokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setAccessTokenExpiresIn(jwtUtil.getAccessTokenSeconds());
        tokenDTO.setRefreshTokenExpiresIn(jwtUtil.getRefreshTokenSeconds());
        return tokenDTO;
    }

    private CurrentAuthDTO toCurrentAuthDTO(User user) {
        CurrentAuthDTO dto = new CurrentAuthDTO();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone());
        dto.setName(user.getName());
        return dto;
    }

    private String refreshKey(String userId, String refreshJti) {
        return authProperties.getRefreshKeyPrefix() + userId + ":" + refreshJti;
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Authorization 格式错误");
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
