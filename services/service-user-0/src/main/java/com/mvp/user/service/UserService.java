package com.mvp.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.user.entity.User;

/**
 * 用户业务 Service。
 *
 */
public interface UserService extends IService<User> {

    String getTest();
}
