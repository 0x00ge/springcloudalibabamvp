package com.mvp.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mvp.common.controller.BaseController;
import com.mvp.common.dto.auth.AuthTokenDTO;
import com.mvp.common.dto.auth.CurrentAuthDTO;
import com.mvp.common.dto.auth.LoginDTO;
import com.mvp.common.vo.ResultVO;
import com.mvp.user.config.AuthProperties;
import com.mvp.user.dto.UserDto;
import com.mvp.user.entity.User;
import com.mvp.user.enums.UserPermission;
import com.mvp.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;
import java.time.Duration;

/**
 * 用户控制器
 *
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserController extends BaseController<User, UserDto> {

    private final UserService userService;
    private final AuthProperties authProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserService userService, AuthProperties authProperties) {
        super(userService);
        this.userService = userService;
        this.authProperties = authProperties;
    }

    /**
     * 用户登录接口。
     *
     * <p>请求路径：POST /user/login</p>
     * <p>是否需要登录：否，登录接口在网关白名单中。</p>
     * <p>传参方式：使用 LoginDTO JSON body 入参。</p>
     *
     * @param loginDTO 登录参数
     * @param response HTTP 响应，用于写入 HttpOnly refreshToken Cookie
     * @return accessToken 信息；refreshToken 通过 HttpOnly Cookie 返回，前端 JS 不能读取
     */
    @PostMapping("/login")
    public ResultVO<AuthTokenDTO> login(@RequestBody @Validated LoginDTO loginDTO,
                                        HttpServletResponse response) {
        AuthTokenDTO tokenDTO = userService.login(loginDTO.getPhone(), loginDTO.getPassword());
        writeRefreshTokenCookie(response, tokenDTO.getRefreshToken(), tokenDTO.getRefreshTokenExpiresIn());
        tokenDTO.setRefreshToken(null);
        return ResultVO.ok(tokenDTO);
    }

    /**
     * 用户登出接口。
     *
     * <p>请求路径：POST /user/logout</p>
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
    public ResultVO<Void> logout(@RequestHeader("Authorization") @NotBlank(message = "Authorization不能为空") String authorization,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String refreshToken = getRefreshTokenCookie(request);
        userService.logout(authorization, refreshToken);
        clearRefreshTokenCookie(response);
        return ResultVO.ok();
    }

    /**
     * 发送注册短信验证码接口。
     *
     * <p>请求路径：POST /user/register/code</p>
     * <p>是否需要登录：否，注册前还没有 accessToken，所以该接口需要在网关白名单中。</p>
     * <p>传参方式：请求参数传参，不使用 JSON body。</p>
     * <p>本地开发说明：当前只把验证码写入 Redis 并打印日志，后续接入短信平台时替换 Service 中的发送逻辑。</p>
     *
     * @param phone 接收验证码的手机号
     * @return 无业务数据，成功表示验证码已生成并保存
     */
    @PostMapping("/register/code")
    public ResultVO<Void> sendRegisterSmsCode(
            @RequestParam @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone) {
        userService.sendRegisterSmsCode(phone);
        return ResultVO.ok();
    }

    /**
     * 用户注册接口。
     *
     * <p>请求路径：POST /user/register</p>
     * <p>是否需要登录：否，注册接口在网关白名单中。</p>
     * <p>传参方式：使用 CurrentAuthDTO JSON body 入参。</p>
     *
     * @param currentAuthDTO 注册入参，包含手机号、姓名、密码、确认密码和短信验证码
     * @return 当前注册成功的用户基础信息，不直接返回 token
     */
    @PostMapping("/register")
    public ResultVO<CurrentAuthDTO> register(@RequestBody @Validated CurrentAuthDTO currentAuthDTO) {
        return ResultVO.ok(userService.register(currentAuthDTO));
    }

    /**
     * 查询当前登录用户接口。
     *
     * <p>请求路径：GET /user/me</p>
     * <p>是否需要登录：是，需要先经过网关 accessToken 校验。</p>
     * <p>业务说明：网关校验 token 成功后，会把用户 ID 写入 X-User-Id 请求头，用户服务直接读取该请求头查询用户。</p>
     *
     * @param userId 网关透传的当前登录用户 ID
     * @return 当前登录用户基础信息
     */
    @GetMapping("/me")
    public ResultVO<CurrentAuthDTO> me(@RequestHeader("X-User-Id") @NotBlank(message = "用户ID不能为空") String userId) {
        return ResultVO.ok(userService.currentUser(userId));
    }

    @Override
    @GetMapping("/{id}")
    public ResultVO<UserDto> getById(@PathVariable String id) {
        return super.getById(id);
    }

    @Override
    @GetMapping("/page")
    public ResultVO<IPage<UserDto>> page(@RequestParam(defaultValue = "1") Long page,
                                         @RequestParam(defaultValue = "10") Long size) {
        HttpServletRequest request = currentRequest();
        String name = request.getParameter("name");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        UserPermission permission = parsePermission(request.getParameter("permission"));
        Integer status = parseStatus(request.getParameter("status"));

        Page<User> queryPage = new Page<>(page, size);
        IPage<UserDto> result = userService.page(queryPage, Wrappers.<User>lambdaQuery()
                .like(StringUtils.hasText(name), User::getName, name)
                .like(StringUtils.hasText(phone), User::getPhone, phone)
                .like(StringUtils.hasText(email), User::getEmail, email)
                .eq(permission != null, User::getPermission, permission)
                .eq(status != null, User::getStatus, status)
                .isNull(User::getDeletedAt)
                .orderByAsc(User::getCreatedAt))
                .convert(this::entity2Dto);
        return ResultVO.ok(result);
    }

    @Override
    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody UserDto dto) {
        encodePlainPassword(dto);
        return super.save(dto);
    }

    @Override
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id, @Valid @RequestBody UserDto dto) {
        encodePlainPassword(dto);
        return super.update(id, dto);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id) {
        return super.delete(id);
    }



    @GetMapping("/test")
    public ResultVO<String> getTest() {
        return ResultVO.ok(userService.getTest());
    }

    private void encodePlainPassword(UserDto dto) {
        String passwordHash = dto.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.startsWith("$2")) {
            return;
        }

        dto.setPasswordHash(passwordEncoder.encode(passwordHash));
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private UserPermission parsePermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            return null;
        }

        return UserPermission.valueOf(permission);
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }

        return Integer.valueOf(status);
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
        String refreshToken = findRefreshTokenCookie(request);
        if (refreshToken != null) {
            return refreshToken;
        }

        throw new IllegalArgumentException("refreshToken Cookie 不存在");
    }

    /**
     * 从请求 Cookie 中查找 refreshToken。
     */
    private String findRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (authProperties.getRefreshTokenCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
