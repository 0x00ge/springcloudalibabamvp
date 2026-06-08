package com.mvp.user.controller;

import com.mvp.model.vo.ResultVO;
import com.mvp.model.dto.auth.AuthTokenDTO;
import com.mvp.model.dto.auth.CurrentAuthDTO;
import com.mvp.user.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户鉴权接口。
 */
@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册。
     */
    @PostMapping("/register")
    public ResultVO<CurrentAuthDTO> register(
            @RequestParam @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @RequestParam @NotBlank(message = "用户名称不能为空")
            @Size(max = 50, message = "用户名称长度不能超过50") String name,
            @RequestParam @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度必须在6到32位之间") String password,
            @RequestParam @NotBlank(message = "确认密码不能为空") String confirmPassword) {
        return ResultVO.ok(authService.register(phone, name, password, confirmPassword));
    }

    /**
     * 登录。
     */
    @PostMapping("/login")
    public ResultVO<AuthTokenDTO> login(
            @RequestParam @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
            @RequestParam @NotBlank(message = "密码不能为空") String password) {
        return ResultVO.ok(authService.login(phone, password));
    }

    /**
     * 使用 refreshToken 刷新双 token。
     */
    @PostMapping("/refresh")
    public ResultVO<AuthTokenDTO> refresh(@RequestParam @NotBlank(message = "refreshToken不能为空") String refreshToken) {
        return ResultVO.ok(authService.refresh(refreshToken));
    }

    /**
     * 登出。
     */
    @PostMapping("/logout")
    public ResultVO<Void> logout(@RequestHeader("Authorization") String authorization,
                                 @RequestParam @NotBlank(message = "refreshToken不能为空") String refreshToken) {
        authService.logout(authorization, refreshToken);
        return ResultVO.ok();
    }

    /**
     * 当前登录用户。
     */
    @GetMapping("/me")
    public ResultVO<CurrentAuthDTO> me(@RequestHeader("X-User-Id") String userId) {
        return ResultVO.ok(authService.currentUser(userId));
    }
}
