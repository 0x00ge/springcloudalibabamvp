package com.mvp.user.controller;

import com.mvp.model.vo.ResultVO;
import com.mvp.model.dto.auth.AuthTokenDTO;
import com.mvp.model.dto.auth.CurrentAuthDTO;
import com.mvp.user.config.AuthProperties;
import com.mvp.user.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 用户鉴权接口。
 *
 * <p>本接口统一处理注册、登录、刷新 token、登出和查询当前登录用户。</p>
 * <p>当前项目约定：注册、登录、刷新接口在网关白名单内，不需要 accessToken；
 * 登出和查询当前用户需要携带 accessToken，并由网关校验后透传用户信息。</p>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;

    public AuthController(AuthService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    /**
     * 发送注册短信验证码接口。
     *
     * <p>请求路径：POST /auth/register/code</p>
     * <p>是否需要登录：否，注册前还没有 accessToken，所以该接口需要在网关白名单中。</p>
     * <p>传参方式：请求参数传参，不使用 JSON body。</p>
     * <p>本地开发说明：当前只把验证码写入 Redis 并打印日志，后续接入短信平台时替换 Service 中的发送逻辑。</p>
     *
     * @param phone 接收验证码的手机号
     * @return 无业务数据，成功表示验证码已生成并保存
     */
    @PostMapping("/register/code")
    public ResultVO<Void> sendRegisterSmsCode(
            @RequestParam @Validated @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone) {
        // 调用鉴权服务生成验证码，并保存到 Redis。
        authService.sendRegisterSmsCode(phone);
        return ResultVO.ok();
    }

    /**
     * 用户注册接口。
     *
     * <p>请求路径：POST /auth/register</p>
     * <p>是否需要登录：否，注册接口在网关白名单中。</p>
     * <p>传参方式：请求参数传参，不使用 JSON body。</p>
     *
     * @param phone 手机号，作为注册和登录的主账号
     * @param name 用户名称
     * @param password 登录密码，服务端会使用 BCrypt 加密后保存
     * @param confirmPassword 确认密码，必须和 password 一致
     * @param smsCode 手机短信验证码
     * @return 当前注册成功的用户基础信息，不直接返回 token
     */
    @PostMapping("/register")
    public ResultVO<CurrentAuthDTO> register(
            @RequestParam @Validated @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @RequestParam @Validated @NotBlank(message = "用户名称不能为空")
            @Size(max = 50, message = "用户名称长度不能超过50") String name,
            @RequestParam @Validated @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度必须在6到32位之间") String password,
            @RequestParam @Validated @NotBlank(message = "确认密码不能为空") String confirmPassword,
            @RequestParam @Validated @NotBlank(message = "验证码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字") String smsCode) {
        // 调用鉴权服务完成验证码校验、注册校验、密码加密和用户入库。
        return ResultVO.ok(authService.register(phone, name, password, confirmPassword, smsCode));
    }

    /**
     * 用户登录接口。
     *
     * <p>请求路径：POST /auth/login</p>
     * <p>是否需要登录：否，登录接口在网关白名单中。</p>
     * <p>传参方式：请求参数传参，不使用 JSON body。</p>
     *
     * @param phone 手机号
     * @param password 登录密码
     * @param response HTTP 响应，用于写入 HttpOnly refreshToken Cookie
     * @return accessToken 信息；refreshToken 通过 HttpOnly Cookie 返回，前端 JS 不能读取
     */
    @PostMapping("/login")
    public ResultVO<AuthTokenDTO> login(
            @RequestParam @Validated @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @RequestParam @Validated @NotBlank(message = "密码不能为空") String password,
            HttpServletResponse response) {
        // 调用鉴权服务完成账号校验、密码校验和双 token 签发。
        AuthTokenDTO tokenDTO = authService.login(phone, password);

        // 把 refreshToken 写入 HttpOnly Cookie，然后清空 DTO 字段，响应体只返回 accessToken。
        writeRefreshTokenCookie(response, tokenDTO.getRefreshToken(), tokenDTO.getRefreshTokenExpiresIn());
        tokenDTO.setRefreshToken(null);
        return ResultVO.ok(tokenDTO);
    }

    /**
     * 刷新双 token 接口。
     *
     * <p>请求路径：POST /auth/refresh</p>
     * <p>是否需要登录：否，刷新接口在网关白名单中。</p>
     * <p>传参方式：浏览器自动携带 HttpOnly refreshToken Cookie，不使用 JSON body，也不使用请求参数传 refreshToken。</p>
     * <p>业务说明：refreshToken 只能使用一次，刷新成功后旧 refreshToken 会失效。</p>
     *
     * @param request HTTP 请求，用于读取 HttpOnly refreshToken Cookie
     * @param response HTTP 响应，用于轮换新的 HttpOnly refreshToken Cookie
     * @return 新的 accessToken 信息
     */
    @PostMapping("/refresh")
    public ResultVO<AuthTokenDTO> refresh(HttpServletRequest request,
                                          HttpServletResponse response) {
        String refreshToken = getRefreshTokenCookie(request);

        // 调用鉴权服务校验 Cookie 中的 refreshToken，并重新签发一组双 token。
        AuthTokenDTO tokenDTO = authService.refresh(refreshToken);

        // 刷新成功后轮换 refreshToken Cookie，旧 refreshToken 已在 Service 中删除白名单。
        writeRefreshTokenCookie(response, tokenDTO.getRefreshToken(), tokenDTO.getRefreshTokenExpiresIn());
        tokenDTO.setRefreshToken(null);
        return ResultVO.ok(tokenDTO);
    }

    /**
     * 用户登出接口。
     *
     * <p>请求路径：POST /auth/logout</p>
     * <p>是否需要登录：是，需要在请求头中携带 Authorization: Bearer accessToken。</p>
     * <p>传参方式：accessToken 放在请求头，refreshToken 由浏览器自动携带 Cookie。</p>
     * <p>业务说明：登出后 accessToken 会进入 Redis 黑名单，refreshToken 会从 Redis 白名单删除。</p>
     *
     * @param authorization Authorization 请求头，格式必须为 Bearer accessToken
     * @param request HTTP 请求，用于读取 HttpOnly refreshToken Cookie
     * @param response HTTP 响应，用于清除 refreshToken Cookie
     * @return 无业务数据，成功即表示当前会话已退出
     */
    @PostMapping("/logout")
    public ResultVO<Void> logout(@RequestHeader("Authorization") @Validated @NotBlank(message = "Authorization不能为空") String authorization,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String refreshToken = getRefreshTokenCookie(request);

        // 调用鉴权服务完成 accessToken 拉黑和 refreshToken 失效处理。
        authService.logout(authorization, refreshToken);

        // 清除浏览器中的 refreshToken Cookie。
        clearRefreshTokenCookie(response);
        return ResultVO.ok();
    }

    /**
     * 查询当前登录用户接口。
     *
     * <p>请求路径：GET /auth/me</p>
     * <p>是否需要登录：是，需要先经过网关 accessToken 校验。</p>
     * <p>业务说明：网关校验 token 成功后，会把用户 ID 写入 X-User-Id 请求头，用户服务直接读取该请求头查询用户。</p>
     *
     * @param userId 网关透传的当前登录用户 ID
     * @return 当前登录用户基础信息
     */
    @GetMapping("/me")
    public ResultVO<CurrentAuthDTO> me(@RequestHeader("X-User-Id") @Validated @NotBlank(message = "用户ID不能为空") String userId) {
        // 根据网关透传的用户 ID 查询当前登录用户信息。
        return ResultVO.ok(authService.currentUser(userId));
    }

    /**
     * 写入 refreshToken Cookie。
     */
    private void writeRefreshTokenCookie(HttpServletResponse response, String refreshToken, Long maxAgeSeconds) {
        ResponseCookie cookie = baseRefreshTokenCookie(refreshToken)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 清除 refreshToken Cookie。
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = baseRefreshTokenCookie("")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * refreshToken Cookie 的公共属性。
     */
    private ResponseCookie.ResponseCookieBuilder baseRefreshTokenCookie(String value) {
        return ResponseCookie.from(authProperties.getRefreshTokenCookieName(), value)
                .httpOnly(true)
                .secure(authProperties.isRefreshTokenCookieSecure())
                .path(authProperties.getRefreshTokenCookiePath())
                .sameSite(authProperties.getRefreshTokenCookieSameSite());
    }

    /**
     * 从请求 Cookie 中读取 refreshToken。
     */
    private String getRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new IllegalArgumentException("refreshToken Cookie 不存在");
        }

        for (Cookie cookie : cookies) {
            if (authProperties.getRefreshTokenCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new IllegalArgumentException("refreshToken Cookie 不存在");
    }
}
