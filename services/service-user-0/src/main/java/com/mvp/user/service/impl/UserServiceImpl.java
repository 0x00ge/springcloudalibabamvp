package com.mvp.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.common.dto.auth.AuthTokenDTO;
import com.mvp.common.dto.auth.CurrentAuthDTO;
import com.mvp.common.enums.ResultCode;
import com.mvp.common.jwt.JwtPayload;
import com.mvp.common.jwt.JwtUtil;
import com.mvp.user.config.AuthProperties;
import com.mvp.user.entity.User;
import com.mvp.user.enums.UserPermission;
import com.mvp.user.mapper.UserMapper;
import com.mvp.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户业务 Service 实现类。
 *
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private static final int USER_STATUS_NORMAL = 1;
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final RedissonClient redissonClient;
    private final AuthProperties authProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(JwtUtil jwtUtil,
                           RedissonClient redissonClient,
                           AuthProperties authProperties) {
        this.jwtUtil = jwtUtil;
        this.redissonClient = redissonClient;
        this.authProperties = authProperties;
    }

    /**
     * 发送注册短信验证码。
     *
     * <p>当前是本地开发实现：生成 6 位验证码写入 Redis，并通过日志打印验证码。
     * 生产环境接入短信平台时，只需要替换这里的发送动作。</p>
     */
    @Override
    public void sendRegisterSmsCode(String phone) {
        boolean exists = count(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone)) > 0;
        if (exists) {
            throw new IllegalArgumentException(ResultCode.PHONE_EXIST.getMessage());
        }

        String smsCode = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        redissonClient.<String>getBucket(registerSmsCodeKey(phone))
                .set(smsCode, Duration.ofSeconds(authProperties.getRegisterSmsCodeSeconds()));

        log.info("注册短信验证码已生成 phone={} smsCode={} ttlSeconds={}",
                phone, smsCode, authProperties.getRegisterSmsCodeSeconds());
    }

    /**
     * 注册新用户。
     *
     * <p>注册接口当前不直接返回 token，注册成功后前端可以继续调用登录接口获取双 token。</p>
     */
    @Override
    public CurrentAuthDTO register(CurrentAuthDTO currentAuthDTO) {
        validateRegisterSmsCode(currentAuthDTO.getPhone(), currentAuthDTO.getSmsCode());

        if (!currentAuthDTO.getPassword().equals(currentAuthDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("两次密码不一致");
        }

        boolean exists = count(Wrappers.<User>lambdaQuery().eq(User::getPhone, currentAuthDTO.getPhone())) > 0;
        if (exists) {
            throw new IllegalArgumentException(ResultCode.PHONE_EXIST.getMessage());
        }

        User user = new User();
        user.setPhone(currentAuthDTO.getPhone());
        user.setPasswordHash(passwordEncoder.encode(currentAuthDTO.getPassword()));
        user.setName(currentAuthDTO.getName());
        user.setGender(0);
        user.setPermission(UserPermission.USER);
        user.setStatus(USER_STATUS_NORMAL);

        save(user);
        redissonClient.getBucket(registerSmsCodeKey(currentAuthDTO.getPhone())).delete();
        log.info("用户注册成功 userId={} phone={}", user.getId(), currentAuthDTO.getPhone());
        return toCurrentAuthDTO(user);
    }

    /**
     * 用户登录。
     *
     * <p>登录成功后会签发一组新的 accessToken 和 refreshToken，并更新最后登录时间。</p>
     */
    @Override
    public AuthTokenDTO login(String phone, String password) {
        User user = getOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone), false);
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
        updateById(user);
        AuthTokenDTO tokenDTO = createAndSaveToken(user.getId());
        log.info("用户登录成功 userId={} phone={}", user.getId(), phone);
        return tokenDTO;
    }

    /**
     * 用户登出。
     *
     * <p>accessToken 本身是无状态 JWT，不能直接删除，所以需要写入 Redis 黑名单；
     * refreshToken 是有状态的白名单 token，直接删除 Redis 记录即可失效。</p>
     */
    @Override
    public void logout(String authorization, String refreshToken) {
        JwtPayload accessPayload = jwtUtil.parseAndValidate(extractBearerToken(authorization), JwtUtil.TYPE_ACCESS);
        JwtPayload refreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);

        long accessTtlSeconds = Math.max(1, accessPayload.getExp() - Instant.now().getEpochSecond());
        redissonClient.<String>getBucket(authProperties.getBlacklistKeyPrefix() + accessPayload.getJti())
                .set(accessPayload.getSub(), Duration.ofSeconds(accessTtlSeconds));

        redissonClient.getBucket(refreshKey(refreshPayload.getSub(), refreshPayload.getJti())).delete();
        log.info("用户登出成功 userId={} accessJti={}", accessPayload.getSub(), accessPayload.getJti());
    }

    /**
     * 查询当前登录用户。
     *
     * <p>userId 由网关校验 accessToken 后透传，用户服务不再重复解析 accessToken。</p>
     */
    @Override
    public CurrentAuthDTO currentUser(String userId) {
        User user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException(ResultCode.USER_NOT_FOUND.getMessage());
        }

        return toCurrentAuthDTO(user);
    }

    @Override
    public String getTest() {
        return "service-user-0: test";
    }

    /**
     * 签发 accessToken 和 refreshToken，并把 refreshToken 写入 Redis 白名单。
     */
    private AuthTokenDTO createAndSaveToken(String userId) {
        String accessToken = jwtUtil.createAccessToken(userId);
        String refreshToken = jwtUtil.createRefreshToken(userId);
        saveRefreshToken(userId, refreshToken);
        return toAuthTokenDTO(accessToken, refreshToken);
    }

    /**
     * 保存 refreshToken Redis 白名单。
     */
    private void saveRefreshToken(String userId, String refreshToken) {
        JwtPayload refreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);
        redissonClient.<String>getBucket(refreshKey(userId, refreshPayload.getJti()))
                .set(userId, Duration.ofSeconds(jwtUtil.getRefreshTokenSeconds()));
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
     * 把用户实体转换成当前登录用户 DTO。
     */
    private CurrentAuthDTO toCurrentAuthDTO(User user) {
        CurrentAuthDTO dto = new CurrentAuthDTO();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone());
        dto.setName(user.getName());
        dto.setPermission(user.getPermission() == null ? UserPermission.USER.getCode() : user.getPermission().getCode());
        return dto;
    }

    /**
     * 生成 refreshToken Redis 白名单 key。
     *
     * <p>key 格式：auth:refresh:{userId}:{refreshJti}</p>
     */
    private String refreshKey(String userId, String refreshJti) {
        return authProperties.getRefreshKeyPrefix() + userId + ":" + refreshJti;
    }

    /**
     * 校验注册短信验证码。
     */
    private void validateRegisterSmsCode(String phone, String smsCode) {
        String smsCodeKey = registerSmsCodeKey(phone);
        RBucket<String> smsCodeBucket = redissonClient.getBucket(smsCodeKey);
        String cachedSmsCode = smsCodeBucket.get();
        if (!StringUtils.hasText(cachedSmsCode)) {
            throw new IllegalArgumentException("验证码已过期");
        }
        if (!cachedSmsCode.equals(smsCode)) {
            throw new IllegalArgumentException("验证码错误");
        }
    }

    /**
     * 生成注册短信验证码 Redis key。
     *
     * <p>key 格式：auth:register:sms:{phone}</p>
     */
    private String registerSmsCodeKey(String phone) {
        return authProperties.getRegisterSmsCodeKeyPrefix() + phone;
    }

    /**
     * 从 Authorization 请求头中提取 Bearer token。
     */
    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Authorization 格式错误");
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
