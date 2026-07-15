package com.mvp.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.common.dto.auth.AuthTokenDTO;
import com.mvp.common.dto.auth.CurrentAuthDTO;
import com.mvp.user.entity.User;

/**
 * 用户业务 Service。
 *
 */
public interface UserService extends IService<User> {

    /**
     * 发送注册短信验证码。
     *
     * <p>本地开发阶段暂时不接真实短信平台，只生成验证码、写入 Redis，并通过日志打印验证码。</p>
     *
     * @param phone 接收验证码的手机号
     */
    void sendRegisterSmsCode(String phone);

    /**
     * 注册新用户。
     *
     * <p>注册时会校验短信验证码、两次密码是否一致、手机号是否已存在，并把明文密码加密为 BCrypt 密文后保存。</p>
     *
     * @param currentAuthDTO 注册入参
     * @return 注册成功后的用户基础信息
     */
    CurrentAuthDTO register(CurrentAuthDTO currentAuthDTO);

    /**
     * 用户登录。
     *
     * <p>登录时会校验用户是否存在、账号状态是否正常、密码是否正确，成功后签发 accessToken 和 refreshToken。</p>
     *
     * @param phone 手机号
     * @param password 明文密码
     * @return 双 token 和对应过期时间
     */
    AuthTokenDTO login(String phone, String password);

    /**
     * 用户登出。
     *
     * <p>登出时会把 accessToken 加入 Redis 黑名单，并删除 refreshToken 白名单记录。</p>
     *
     * @param authorization Authorization 请求头，格式为 Bearer accessToken
     * @param refreshToken refreshToken
     */
    void logout(String authorization, String refreshToken);

    /**
     * 查询当前登录用户。
     *
     * <p>userId 来自网关校验 accessToken 后透传的 X-User-Id 请求头。</p>
     *
     * @param userId 当前登录用户 ID
     * @return 当前登录用户基础信息
     */
    CurrentAuthDTO currentUser(String userId);

    String getTest();
}
