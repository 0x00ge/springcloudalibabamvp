package com.mvp.common.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户信息和注册入参。
 *
 * <p>注册接口会使用 phone、name、password、confirmPassword、smsCode；
 * 查询当前用户和注册成功响应只返回 id、name、phone。</p>
 */
@Data
public class CurrentAuthDTO {

    /**
     * 用户ID。
     */
    private String id;

    /**
     * 用户名称。
     */
    @NotBlank(message = "用户名称不能为空")
    @Size(max = 50, message = "用户名称长度不能超过50")
    private String name;

    /**
     * 手机号。
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 当前用户权限。
     *
     * <p>这里使用字符串而不是 user 服务里的枚举类型，是为了让 common 模块不反向依赖具体业务模块。
     * 当前取值为 ADMIN、USER，前端 authStore 根据该字段判断是否展示管理员入口。</p>
     */
    private String permission;

    /**
     * 登录密码。
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6到32位之间")
    private String password;

    /**
     * 确认密码。
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 手机短信验证码。
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    private String smsCode;
}
