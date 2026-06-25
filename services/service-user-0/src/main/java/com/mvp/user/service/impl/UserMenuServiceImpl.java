package com.mvp.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.user.dto.UserMenuDto;
import com.mvp.user.entity.User;
import com.mvp.user.entity.UserMenu;
import com.mvp.user.mapper.UserMenuMapper;
import com.mvp.user.service.UserMenuService;
import com.mvp.user.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户菜单业务 Service 实现类。
 */
@Service
public class UserMenuServiceImpl extends ServiceImpl<UserMenuMapper, UserMenu>
        implements UserMenuService {

    private static final Set<String> ENABLED_MENU_PATHS = Set.of("/system", "/home/user");

    private final UserService userService;

    public UserMenuServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<UserMenuDto> treeByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        User user = getExistingUser(userId);
        ensureDefaultMenus(user);

        return treeByTargetUserId(userId);
    }

    private List<UserMenuDto> treeByTargetUserId(String userId) {
        List<UserMenu> menus = list(Wrappers.<UserMenu>lambdaQuery()
                .eq(UserMenu::getUserId, userId)
                .isNull(UserMenu::getDeletedAt)
                .in(UserMenu::getPath, ENABLED_MENU_PATHS)
                .orderByAsc(UserMenu::getSortOrder)
                .orderByAsc(UserMenu::getCreatedAt));

        return buildTree(menus);
    }

    private void ensureDefaultMenus(User user) {
        long count = count(Wrappers.<UserMenu>lambdaQuery()
                .eq(UserMenu::getUserId, user.getId())
                .isNull(UserMenu::getDeletedAt));
        if (count > 0) {
            return;
        }

        UserMenu system = newMenu(user.getId(), null, 1, 10, "系统管理", "/system", "Setting");
        save(system);
        save(newMenu(user.getId(), system.getId(), 2, 20, "用户管理", "/home/user", "UserFilled"));
    }

    private UserMenu newMenu(String userId,
                             String parentId,
                             Integer level,
                             Integer sortOrder,
                             String title,
                             String path,
                             String icon) {
        UserMenu menu = new UserMenu();
        menu.setUserId(userId);
        menu.setParentId(parentId);
        menu.setLevel(level);
        menu.setSortOrder(sortOrder);
        menu.setTitle(title);
        menu.setPath(path);
        menu.setIcon(icon);
        Date now = new Date();
        menu.setCreatedAt(now);
        menu.setUpdatedAt(now);
        return menu;
    }

    private User getExistingUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        User user = userService.getById(userId);
        if (user == null || user.getDeletedAt() != null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return user;
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
