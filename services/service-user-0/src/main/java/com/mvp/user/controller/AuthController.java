package com.mvp.user.controller;

import com.mvp.common.enums.ResultCode;
import com.mvp.common.dto.auth.AuthTokenDTO;
import com.mvp.common.vo.ResultVO;
import com.mvp.user.config.AuthProperties;
import com.mvp.user.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token 接口。
 *
 * <p>本接口只处理独立的 token 能力；登录、登出、注册和当前用户查询由 {@link UserController} 提供。</p>
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
     * 刷新 accessToken 接口。
     *
     * <p>请求路径：POST /auth/refresh</p>
     * <p>是否需要登录：否，刷新接口在网关白名单中。</p>
     * <p>传参方式：浏览器自动携带 HttpOnly refreshToken Cookie，不使用 JSON body，也不使用请求参数传 refreshToken。</p>
     * <p>业务说明：refreshToken 使用固定会话有效期，刷新 accessToken 时不续期。</p>
     *
     * @param request HTTP 请求，用于读取 HttpOnly refreshToken Cookie
     * @return 新的 accessToken 信息
     */
    @PostMapping("/refresh")
    public ResultVO<AuthTokenDTO> refresh(HttpServletRequest request) {
        String refreshToken = findRefreshTokenCookie(request);
        if (refreshToken == null) {
            return ResultVO.fail(ResultCode.PARAM_ERROR.getCode(), "refreshToken Cookie 不存在");
        }

        // 调用鉴权服务校验 Cookie 中的 refreshToken，并重新签发一组双 token。
        AuthTokenDTO tokenDTO = authService.refresh(refreshToken);

        // refreshToken 使用固定会话有效期，刷新 accessToken 时不重写 Cookie，避免 Cookie Max-Age 被续期。
        tokenDTO.setRefreshToken(null);
        return ResultVO.ok(tokenDTO);
    }

    /**
     * 从请求 Cookie 中查找 refreshToken。
     *
     * <p>refresh 接口允许未登录用户刷新登录页时没有 Cookie，此时返回 null 交给接口返回统一业务失败。</p>
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
