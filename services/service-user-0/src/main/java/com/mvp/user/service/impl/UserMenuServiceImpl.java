package com.mvp.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.user.dto.UserMenuDto;
import com.mvp.user.entity.UserMenu;
import com.mvp.user.mapper.UserMenuMapper;
import com.mvp.user.service.UserMenuService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户菜单业务 Service 实现类。
 */
@Service
public class UserMenuServiceImpl extends ServiceImpl<UserMenuMapper, UserMenu>
        implements UserMenuService {

    @Override
    public List<UserMenuDto> treeByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        List<UserMenu> menus = list(Wrappers.<UserMenu>lambdaQuery()
                .eq(UserMenu::getUserId, userId)
                .isNull(UserMenu::getDeletedAt)
                .orderByAsc(UserMenu::getLevel)
                .orderByAsc(UserMenu::getSortOrder)
                .orderByAsc(UserMenu::getCreatedAt));

        return buildTree(menus);
    }

    private List<UserMenuDto> buildTree(List<UserMenu> menus) {
        Map<String, UserMenuDto> menuMap = new LinkedHashMap<>();
        List<UserMenuDto> roots = new ArrayList<>();

        for (UserMenu menu : menus) {
            UserMenuDto dto = toDto(menu);
            dto.setChildren(new ArrayList<>());
            menuMap.put(dto.getId(), dto);
        }

        for (UserMenuDto dto : menuMap.values()) {
            String parentId = dto.getParentId();
            UserMenuDto parent = StringUtils.hasText(parentId) ? menuMap.get(parentId) : null;
            if (parent == null) {
                roots.add(dto);
            } else {
                parent.getChildren().add(dto);
            }
        }

        return roots;
    }

    private UserMenuDto toDto(UserMenu menu) {
        UserMenuDto dto = new UserMenuDto();
        BeanUtils.copyProperties(menu, dto);
        return dto;
    }
}
