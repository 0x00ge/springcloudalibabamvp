package com.mvp.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.common.uuidv7.UuidV7Util;
import com.mvp.user.dto.MenuDto;
import com.mvp.user.entity.Menu;
import com.mvp.user.mapper.MenuMapper;
import com.mvp.user.service.MenuService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 菜单 Service 实现。
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    private static final int ROOT_LEVEL = 1;
    private static final int DEFAULT_SORT_ORDER = 0;

    @Override
    public List<MenuDto> tree(String userId) {
        List<Menu> menus = list(Wrappers.<Menu>lambdaQuery()
                .eq(Menu::getUserId, userId)
                .isNull(Menu::getDeletedAt)
                .orderByAsc(Menu::getLevel)
                .orderByAsc(Menu::getSortOrder)
                .orderByAsc(Menu::getCreatedAt));
        return buildTree(menus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(MenuDto dto) {
        Menu menu = dto2Entity(dto);
        Menu parent = findParent(menu.getUserId(), menu.getParentId());
        menu.setLevel(parent == null ? ROOT_LEVEL : safeLevel(parent.getLevel()) + 1);
        menu.setSortOrder(menu.getSortOrder() == null ? DEFAULT_SORT_ORDER : menu.getSortOrder());
        menu.setDeletedAt(null);
        save(menu);
        return menu.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, MenuDto dto) {
        Menu exist = getAliveMenu(id, dto.getUserId());
        Menu parent = findParent(dto.getUserId(), dto.getParentId());
        if (parent != null && Objects.equals(parent.getId(), id)) {
            throw new IllegalArgumentException("父菜单不能是自己");
        }
        if (parent != null && isDescendant(parent.getId(), id, dto.getUserId())) {
            throw new IllegalArgumentException("父菜单不能是当前菜单的子菜单");
        }

        exist.setParentId(StringUtils.hasText(dto.getParentId()) ? dto.getParentId() : null);
        exist.setTitle(dto.getTitle());
        exist.setPath(dto.getPath());
        exist.setIcon(dto.getIcon());
        exist.setSortOrder(dto.getSortOrder() == null ? DEFAULT_SORT_ORDER : dto.getSortOrder());
        exist.setLevel(parent == null ? ROOT_LEVEL : safeLevel(parent.getLevel()) + 1);
        updateById(exist);
        refreshChildrenLevel(exist.getId(), exist.getUserId(), exist.getLevel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDelete(String id, String userId) {
        getAliveMenu(id, userId);
        Set<String> ids = collectSelfAndChildrenIds(id, userId);
        Date deletedAt = new Date();
        for (String menuId : ids) {
            Menu menu = new Menu();
            menu.setId(menuId);
            menu.setDeletedAt(deletedAt);
            updateById(menu);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MenuDto> resetDefault(String userId) {
        remove(Wrappers.<Menu>lambdaQuery().eq(Menu::getUserId, userId));

        String systemMenuId = UuidV7Util.generate();
        Menu systemMenu = defaultMenu(systemMenuId, null, userId, "系统管理", "/home/system", null, ROOT_LEVEL, 1);
        Menu userMenu = defaultMenu(UuidV7Util.generate(), systemMenuId, userId, "用户管理", "/home/user", null, ROOT_LEVEL + 1, 1);
        Menu menuMenu = defaultMenu(UuidV7Util.generate(), systemMenuId, userId, "菜单管理", "/home/menu", null, ROOT_LEVEL + 1, 2);
        saveBatch(List.of(systemMenu, userMenu, menuMenu));
        return tree(userId);
    }

    private Menu getAliveMenu(String id, String userId) {
        Menu menu = getOne(Wrappers.<Menu>lambdaQuery()
                .eq(Menu::getId, id)
                .eq(Menu::getUserId, userId)
                .isNull(Menu::getDeletedAt), false);
        if (menu == null) {
            throw new IllegalArgumentException("菜单不存在");
        }
        return menu;
    }

    private Menu findParent(String userId, String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return null;
        }
        return getAliveMenu(parentId, userId);
    }

    private boolean isDescendant(String possibleChildId, String parentId, String userId) {
        Set<String> childIds = collectSelfAndChildrenIds(parentId, userId);
        return childIds.contains(possibleChildId);
    }

    private Set<String> collectSelfAndChildrenIds(String id, String userId) {
        List<Menu> menus = list(Wrappers.<Menu>lambdaQuery()
                .eq(Menu::getUserId, userId)
                .isNull(Menu::getDeletedAt));
        Map<String, List<Menu>> childrenMap = groupByParentId(menus);
        Set<String> ids = new HashSet<>();
        collectIds(id, childrenMap, ids);
        return ids;
    }

    private void collectIds(String id, Map<String, List<Menu>> childrenMap, Set<String> ids) {
        if (!ids.add(id)) {
            return;
        }
        for (Menu child : childrenMap.getOrDefault(id, List.of())) {
            collectIds(child.getId(), childrenMap, ids);
        }
    }

    private void refreshChildrenLevel(String parentId, String userId, Integer parentLevel) {
        List<Menu> children = list(Wrappers.<Menu>lambdaQuery()
                .eq(Menu::getUserId, userId)
                .eq(Menu::getParentId, parentId)
                .isNull(Menu::getDeletedAt));
        for (Menu child : children) {
            child.setLevel(safeLevel(parentLevel) + 1);
            updateById(child);
            refreshChildrenLevel(child.getId(), userId, child.getLevel());
        }
    }

    private List<MenuDto> buildTree(List<Menu> menus) {
        Map<String, MenuDto> dtoMap = new LinkedHashMap<>();
        for (Menu menu : menus) {
            MenuDto dto = entity2Dto(menu);
            dto.setChildren(new ArrayList<>());
            dtoMap.put(dto.getId(), dto);
        }

        List<MenuDto> roots = new ArrayList<>();
        for (MenuDto dto : dtoMap.values()) {
            if (!StringUtils.hasText(dto.getParentId()) || !dtoMap.containsKey(dto.getParentId())) {
                roots.add(dto);
            } else {
                dtoMap.get(dto.getParentId()).getChildren().add(dto);
            }
        }

        sortTree(roots);
        return roots;
    }

    private void sortTree(List<MenuDto> menus) {
        menus.sort(Comparator
                .comparing((MenuDto menu) -> menu.getSortOrder() == null ? DEFAULT_SORT_ORDER : menu.getSortOrder())
                .thenComparing(MenuDto::getCreatedAt, Comparator.nullsLast(Date::compareTo)));
        for (MenuDto menu : menus) {
            sortTree(menu.getChildren());
        }
    }

    private Map<String, List<Menu>> groupByParentId(List<Menu> menus) {
        Map<String, List<Menu>> childrenMap = new LinkedHashMap<>();
        for (Menu menu : menus) {
            if (!StringUtils.hasText(menu.getParentId())) {
                continue;
            }
            childrenMap.computeIfAbsent(menu.getParentId(), key -> new ArrayList<>()).add(menu);
        }
        return childrenMap;
    }

    private Menu defaultMenu(String id, String parentId, String userId, String title, String path, String icon, Integer level, Integer sortOrder) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setUserId(userId);
        menu.setTitle(title);
        menu.setPath(path);
        menu.setIcon(icon);
        menu.setLevel(level);
        menu.setSortOrder(sortOrder);
        return menu;
    }

    private int safeLevel(Integer level) {
        return level == null ? ROOT_LEVEL : level;
    }

    private Menu dto2Entity(MenuDto dto) {
        Menu menu = new Menu();
        BeanUtils.copyProperties(dto, menu, "children");
        if (!StringUtils.hasText(menu.getParentId())) {
            menu.setParentId(null);
        }
        return menu;
    }

    private MenuDto entity2Dto(Menu menu) {
        MenuDto dto = new MenuDto();
        BeanUtils.copyProperties(menu, dto);
        return dto;
    }
}
