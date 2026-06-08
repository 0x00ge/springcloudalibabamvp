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
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户鉴权 Service 实现。
 *
 * <p>这里承载鉴权中心的核心业务逻辑：</p>
 * <p>1. 用户注册时创建账号并加密密码；</p>
 * <p>2. 用户登录时校验账号密码并签发双 token；</p>
 * <p>3. refreshToken 使用 Redis 白名单控制一次性刷新；</p>
 * <p>4. accessToken 登出后写入 Redis 黑名单，避免未过期 token 继续访问。</p>
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

    /**
     * 发送注册短信验证码。
     *
     * <p>当前是本地开发实现：生成 6 位验证码写入 Redis，并通过日志打印验证码。
     * 生产环境接入短信平台时，只需要替换这里的发送动作。</p>
     */
    @Override
    public void sendRegisterSmsCode(String phone) {
        // 1. 注册前先校验手机号是否已经存在，避免给已注册手机号重复发送注册验证码。
        boolean exists = userService.count(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone)) > 0;
        if (exists) {
            throw new IllegalArgumentException(ResultCode.PHONE_EXIST.getMessage());
        }

        // 2. 生成 6 位数字验证码。
        String smsCode = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // 3. 写入 Redis，并设置过期时间。
        stringRedisTemplate.opsForValue().set(
                registerSmsCodeKey(phone),
                smsCode,
                Duration.ofSeconds(authProperties.getRegisterSmsCodeSeconds())
        );

        // 4. 本地开发用日志模拟短信发送；生产环境不要在日志中打印验证码。
        log.info("注册短信验证码已生成 phone={} smsCode={} ttlSeconds={}",
                phone, smsCode, authProperties.getRegisterSmsCodeSeconds());
    }

    /**
     * 注册新用户。
     *
     * <p>注册接口当前不直接返回 token，注册成功后前端可以继续调用登录接口获取双 token。</p>
     */
    @Override
    public CurrentAuthDTO register(String phone, String name, String password, String confirmPassword, String smsCode) {
        // 1. 校验短信验证码，验证码错误或过期都不允许注册。
        validateRegisterSmsCode(phone, smsCode);

        // 2. 校验两次密码是否一致，避免用户误输入。
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次密码不一致");
        }

        // 3. 校验手机号是否已经注册，手机号在用户表中必须唯一。
        boolean exists = userService.count(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone)) > 0;
        if (exists) {
            throw new IllegalArgumentException(ResultCode.PHONE_EXIST.getMessage());
        }

        // 4. 构建用户实体，密码必须先 BCrypt 加密，再保存到数据库。
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setGender(0);
        user.setStatus(USER_STATUS_NORMAL);

        // 5. 保存用户，并删除已使用的验证码，避免同一个验证码重复注册。
        userService.save(user);
        stringRedisTemplate.delete(registerSmsCodeKey(phone));
        log.info("用户注册成功 userId={} phone={}", user.getId(), phone);
        return toCurrentAuthDTO(user);
    }

    /**
     * 用户登录。
     *
     * <p>登录成功后会签发一组新的 accessToken 和 refreshToken，并更新最后登录时间。</p>
     */
    @Override
    public AuthTokenDTO login(String phone, String password) {
        // 1. 按手机号查询用户，手机号是当前项目的登录主账号。
        User user = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone), false);
        if (user == null) {
            throw new IllegalArgumentException(ResultCode.USER_NOT_FOUND.getMessage());
        }

        // 2. 校验账号状态，只有正常状态的账号允许登录。
        if (user.getStatus() == null || user.getStatus() != USER_STATUS_NORMAL) {
            throw new IllegalArgumentException(ResultCode.ACCOUNT_DISABLED.getMessage());
        }

        // 3. 使用 BCrypt 校验明文密码和数据库中的密码密文是否匹配。
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("手机号或密码错误");
        }

        // 4. 更新最后登录时间，并签发双 token。
        user.setLastLoginAt(new Date());
        userService.updateById(user);
        AuthTokenDTO tokenDTO = createAndSaveToken(user.getId());
        log.info("用户登录成功 userId={} phone={}", user.getId(), phone);
        return tokenDTO;
    }

    /**
     * 使用 refreshToken 刷新双 token。
     *
     * <p>refreshToken 采用一次性使用策略：每次刷新成功后，旧 refreshToken 会从 Redis 删除。</p>
     */
    @Override
    public AuthTokenDTO refresh(String refreshToken) {
        // 1. 校验 refreshToken 签名、过期时间和 token 类型。
        JwtPayload oldRefreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);

        // 2. 校验 refreshToken 是否仍在 Redis 白名单中，不在白名单说明已过期、已登出或已被使用过。
        String refreshKey = refreshKey(oldRefreshPayload.getSub(), oldRefreshPayload.getJti());
        Boolean exists = stringRedisTemplate.hasKey(refreshKey);
        if (!Boolean.TRUE.equals(exists)) {
            throw new IllegalArgumentException("refreshToken 已失效");
        }

        // 3. 删除旧 refreshToken 白名单记录，保证 refreshToken 只能使用一次。
        stringRedisTemplate.delete(refreshKey);

        // 4. 重新签发双 token，并保存新的 refreshToken 白名单记录。
        AuthTokenDTO tokenDTO = createAndSaveToken(oldRefreshPayload.getSub());
        log.info("刷新双 token 成功 userId={}", oldRefreshPayload.getSub());
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
        // 1. 从 Authorization 请求头中提取 accessToken，并校验 accessToken。
        JwtPayload accessPayload = jwtUtil.parseAndValidate(extractBearerToken(authorization), JwtUtil.TYPE_ACCESS);

        // 2. 校验 refreshToken，确保前端传入的是合法的 refreshToken。
        JwtPayload refreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);

        // 3. 按 accessToken 剩余有效期写入黑名单，过期后 Redis 会自动删除该黑名单 key。
        long accessTtlSeconds = Math.max(1, accessPayload.getExp() - Instant.now().getEpochSecond());
        stringRedisTemplate.opsForValue().set(
                authProperties.getBlacklistKeyPrefix() + accessPayload.getJti(),
                accessPayload.getSub(),
                Duration.ofSeconds(accessTtlSeconds)
        );

        // 4. 删除 refreshToken 白名单记录，阻止它继续刷新新的双 token。
        stringRedisTemplate.delete(refreshKey(refreshPayload.getSub(), refreshPayload.getJti()));
        log.info("用户登出成功 userId={} accessJti={}", accessPayload.getSub(), accessPayload.getJti());
    }

    /**
     * 查询当前登录用户。
     *
     * <p>userId 由网关校验 accessToken 后透传，用户服务不再重复解析 accessToken。</p>
     */
    @Override
    public CurrentAuthDTO currentUser(String userId) {
        // 1. 根据网关透传的用户 ID 查询数据库。
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException(ResultCode.USER_NOT_FOUND.getMessage());
        }

        // 2. 转换为鉴权接口需要返回的当前用户 DTO。
        return toCurrentAuthDTO(user);
    }

    /**
     * 签发 accessToken 和 refreshToken，并把 refreshToken 写入 Redis 白名单。
     */
    private AuthTokenDTO createAndSaveToken(String userId) {
        // 1. 使用 JWT 工具类分别生成访问 token 和刷新 token。
        String accessToken = jwtUtil.createAccessToken(userId);
        String refreshToken = jwtUtil.createRefreshToken(userId);

        // 2. 保存 refreshToken 白名单，过期时间和 refreshToken 自身过期时间保持一致。
        saveRefreshToken(userId, refreshToken);

        // 3. refreshToken 先放入 DTO，Controller 写入 Cookie 后会在响应体返回前清空该字段。
        return toAuthTokenDTO(accessToken, refreshToken);
    }

    /**
     * 保存 refreshToken Redis 白名单。
     */
    private void saveRefreshToken(String userId, String refreshToken) {
        JwtPayload refreshPayload = jwtUtil.parseAndValidate(refreshToken, JwtUtil.TYPE_REFRESH);
        stringRedisTemplate.opsForValue().set(
                refreshKey(userId, refreshPayload.getJti()),
                userId,
                Duration.ofSeconds(jwtUtil.getRefreshTokenSeconds())
        );
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
        String cachedSmsCode = stringRedisTemplate.opsForValue().get(smsCodeKey);
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
