package com.mvp.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mvp.common.controller.BaseController;
import com.mvp.common.enums.ResultCode;
import com.mvp.common.vo.ResultVO;
import com.mvp.user.dto.MenuDto;
import com.mvp.user.entity.Menu;
import com.mvp.user.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.util.List;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/**
 * 菜单控制器。
 */
@Validated
@RestController
@RequestMapping("/menu")
public class MenuController extends BaseController<Menu, MenuDto> {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        super(menuService);
        this.menuService = menuService;
    }

    @Override
    @GetMapping("/{id}")
    public ResultVO<MenuDto> getById(@PathVariable String id) {
        Menu menu = menuService.getOne(Wrappers.<Menu>lambdaQuery()
                .eq(Menu::getId, id)
                .isNull(Menu::getDeletedAt), false);
        if (menu == null) {
            return ResultVO.build(ResultCode.NOT_FOUND);
        }
        return ResultVO.ok(entity2Dto(menu));
    }

    @Override
    @GetMapping("/page")
    public ResultVO<IPage<MenuDto>> page(@RequestParam(defaultValue = "1") Long page,
                                         @RequestParam(defaultValue = "10") Long size) {
        HttpServletRequest request = currentRequest();
        String userId = resolveUserId(request.getParameter("userId"), currentUserId());
        String title = request.getParameter("title");
        String path = request.getParameter("path");

        IPage<MenuDto> result = menuService.page(new Page<>(page, size), Wrappers.<Menu>lambdaQuery()
                .eq(Menu::getUserId, userId)
                .like(StringUtils.hasText(title), Menu::getTitle, title)
                .like(StringUtils.hasText(path), Menu::getPath, path)
                .isNull(Menu::getDeletedAt)
                .orderByAsc(Menu::getLevel)
                .orderByAsc(Menu::getSortOrder)
                .orderByAsc(Menu::getCreatedAt))
                .convert(this::entity2Dto);
        return ResultVO.ok(result);
    }

    @GetMapping("/tree")
    public ResultVO<List<MenuDto>> tree(@RequestParam(required = false) String userId,
                                        @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        return ResultVO.ok(menuService.tree(resolveUserId(userId, currentUserId)));
    }

    @Override
    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody MenuDto dto) {
        dto.setUserId(resolveUserId(dto.getUserId(), currentUserId()));
        return ResultVO.ok(menuService.create(dto));
    }

    @Override
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id, @Valid @RequestBody MenuDto dto) {
        dto.setUserId(resolveUserId(dto.getUserId(), currentUserId()));
        menuService.update(id, dto);
        return ResultVO.ok();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id) {
        menuService.softDelete(id, resolveUserId(currentRequest().getParameter("userId"), currentUserId()));
        return ResultVO.ok();
    }

    @PostMapping("/reset")
    public ResultVO<List<MenuDto>> reset(@RequestParam(required = false) String userId,
                                         @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        return ResultVO.ok(menuService.resetDefault(resolveUserId(userId, currentUserId)));
    }

    private String resolveUserId(String userId, String currentUserId) {
        if (hasText(userId)) {
            return userId;
        }
        if (hasText(currentUserId)) {
            return currentUserId;
        }
        throw new IllegalArgumentException("用户ID不能为空");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String currentUserId() {
        return currentRequest().getHeader("X-User-Id");
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
