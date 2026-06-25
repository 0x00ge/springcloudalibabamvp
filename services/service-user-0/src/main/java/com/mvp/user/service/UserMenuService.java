package com.mvp.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.user.dto.UserMenuDto;
import com.mvp.user.entity.UserMenu;

import java.util.List;

/**
 * 用户菜单业务 Service。
 */
public interface UserMenuService extends IService<UserMenu> {

    /**
     * 查询指定用户的菜单树。
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    List<UserMenuDto> treeByUserId(String userId);
}
