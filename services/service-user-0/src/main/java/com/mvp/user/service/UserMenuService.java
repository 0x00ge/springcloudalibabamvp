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

    /**
     * 管理端查询指定用户的菜单树。
     *
     * @param operatorUserId 当前操作人用户ID，必须是管理员
     * @param targetUserId   被管理的用户ID
     * @return 菜单树
     */
    List<UserMenuDto> treeCheck(String operatorUserId, String targetUserId);

    /**
     * 管理端新增菜单。
     *
     * @param operatorUserId 当前操作人用户ID，必须是管理员
     * @param dto            菜单表单
     * @return 新菜单ID
     */
    String createMenu(String operatorUserId, UserMenuDto dto);

    /**
     * 管理端修改菜单。
     *
     * @param operatorUserId 当前操作人用户ID，必须是管理员
     * @param id             菜单ID
     * @param dto            菜单表单
     */
    void updateMenu(String operatorUserId, String id, UserMenuDto dto);

    /**
     * 管理端删除菜单。
     *
     * @param operatorUserId 当前操作人用户ID，必须是管理员
     * @param id             菜单ID
     */
    void deleteMenu(String operatorUserId, String id);
}
