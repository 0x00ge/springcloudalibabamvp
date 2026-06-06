package com.mvp.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.model.entity.user.User;
import com.mvp.user.service.UserService;
import com.mvp.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户业务 Service 实现类。
 *
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Override
    public String getTest() {
        return "service-user-0: test";
    }
}
