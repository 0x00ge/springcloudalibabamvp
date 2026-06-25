package com.mvp.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.user.dto.UserMenuDto;
import com.mvp.user.entity.User;
import com.mvp.user.entity.UserMenu;
import com.mvp.user.enums.UserPermission;
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

/**
 * 用户菜单业务 Service 实现类。
 */
@Service
public class UserMenuServiceImpl extends ServiceImpl<UserMenuMapper, UserMenu>
        implements UserMenuService {

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

    @Override
    public List<UserMenuDto> treeCheck(String operatorUserId, String targetUserId) {
        requireAdmin(operatorUserId);
        String queryUserId = StringUtils.hasText(targetUserId) ? targetUserId : operatorUserId;
        getExistingUser(queryUserId);

        return treeByTargetUserId(queryUserId);
    }

    @Override
    public String createMenu(String operatorUserId, UserMenuDto dto) {
        requireAdmin(operatorUserId);
        User targetUser = getExistingUser(dto.getUserId());

        UserMenu menu = toEntity(dto);
        menu.setId(null);
        menu.setDeletedAt(null);
        fillParentLevel(menu);
        Date now = new Date();
        menu.setCreatedAt(now);
        menu.setUpdatedAt(now);

        save(menu);
        ensureDefaultMenus(targetUser);

        return menu.getId();
    }

    @Override
    public void updateMenu(String operatorUserId, String id, UserMenuDto dto) {
        requireAdmin(operatorUserId);
        UserMenu exist = getActiveMenu(id);
        getExistingUser(dto.getUserId());

        UserMenu menu = toEntity(dto);
        menu.setId(id);
        menu.setCreatedAt(exist.getCreatedAt());
        menu.setUpdatedAt(new Date());
        menu.setDeletedAt(null);
        fillParentLevel(menu);

        updateById(menu);
    }

    @Override
    public void deleteMenu(String operatorUserId, String id) {
        requireAdmin(operatorUserId);
        UserMenu menu = getActiveMenu(id);
        Date now = new Date();

        List<UserMenu> menus = list(Wrappers.<UserMenu>lambdaQuery()
                .eq(UserMenu::getUserId, menu.getUserId())
                .isNull(UserMenu::getDeletedAt));

        List<String> deleteIds = collectSelfAndChildrenIds(id, menus);
        if (deleteIds.isEmpty()) {
            throw new IllegalArgumentException("菜单不存在");
        }

        for (String deleteId : deleteIds) {
            UserMenu deletedMenu = new UserMenu();
            deletedMenu.setId(deleteId);
            deletedMenu.setDeletedAt(now);
            deletedMenu.setUpdatedAt(now);
            updateById(deletedMenu);
        }
    }

    private List<UserMenuDto> treeByTargetUserId(String userId) {
        List<UserMenu> menus = list(Wrappers.<UserMenu>lambdaQuery()
                .eq(UserMenu::getUserId, userId)
                .isNull(UserMenu::getDeletedAt)
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
        save(newMenu(user.getId(), system.getId(), 2, 30, "部门管理", "/home/department", "OfficeBuilding"));

        if (isAdmin(user)) {
            save(newMenu(user.getId(), system.getId(), 2, 40, "菜单管理", "/home/menu", "Menu"));
        }
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

    private void fillParentLevel(UserMenu menu) {
        String parentId = menu.getParentId();
        if (!StringUtils.hasText(parentId)) {
            menu.setParentId(null);
            menu.setLevel(1);
            return;
        }

        UserMenu parent = getActiveMenu(parentId);
        if (!parent.getUserId().equals(menu.getUserId())) {
            throw new IllegalArgumentException("父菜单不属于当前用户");
        }
        if (menu.getId() != null && menu.getId().equals(parentId)) {
            throw new IllegalArgumentException("父菜单不能选择自身");
        }

        menu.setLevel(parent.getLevel() + 1);
    }

    private List<String> collectSelfAndChildrenIds(String id, List<UserMenu> menus) {
        List<String> ids = new ArrayList<>();
        collectSelfAndChildrenIds(id, menus, ids);
        return ids;
    }

    private void collectSelfAndChildrenIds(String id, List<UserMenu> menus, List<String> ids) {
        ids.add(id);
        for (UserMenu menu : menus) {
            if (id.equals(menu.getParentId())) {
                collectSelfAndChildrenIds(menu.getId(), menus, ids);
            }
        }
    }

    private UserMenu getActiveMenu(String id) {
        UserMenu menu = getOne(Wrappers.<UserMenu>lambdaQuery()
                .eq(UserMenu::getId, id)
                .isNull(UserMenu::getDeletedAt), false);
        if (menu == null) {
            throw new IllegalArgumentException("菜单不存在");
        }

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

    private void requireAdmin(String userId) {
        User user = getExistingUser(userId);
        if (!isAdmin(user)) {
            throw new IllegalArgumentException("只有管理员可以操作菜单管理");
        }
    }

    private boolean isAdmin(User user) {
        return UserPermission.ADMIN.equals(user.getPermission());
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

    private UserMenu toEntity(UserMenuDto dto) {
        UserMenu menu = new UserMenu();
        BeanUtils.copyProperties(dto, menu);
        return menu;
    }
}
