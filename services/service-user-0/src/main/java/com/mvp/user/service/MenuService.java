package com.mvp.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.user.dto.MenuDto;
import com.mvp.user.entity.Menu;
import java.util.List;

/**
 * 菜单 Service。
 */
public interface MenuService extends IService<Menu> {

    /**
     * 查询指定用户的菜单树。
     */
    List<MenuDto> tree(String userId);

    /**
     * 新增菜单。
     */
    String create(MenuDto dto);

    /**
     * 修改菜单。
     */
    void update(String id, MenuDto dto);

    /**
     * 软删除菜单及其所有子菜单。
     */
    void softDelete(String id, String userId);
}
