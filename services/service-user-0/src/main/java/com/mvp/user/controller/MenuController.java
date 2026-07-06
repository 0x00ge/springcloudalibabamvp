package com.mvp.user.controller;

import com.mvp.common.vo.ResultVO;
import com.mvp.user.dto.MenuDto;
import com.mvp.user.service.MenuService;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.util.List;
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

/**
 * 菜单控制器。
 */
@Validated
@RestController
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/tree")
    public ResultVO<List<MenuDto>> tree(@RequestParam(required = false) String userId,
                                        @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        return ResultVO.ok(menuService.tree(resolveUserId(userId, currentUserId)));
    }

    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody MenuDto dto,
                                       @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        dto.setUserId(resolveUserId(dto.getUserId(), currentUserId));
        return ResultVO.ok(menuService.create(dto));
    }

    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id,
                                 @Valid @RequestBody MenuDto dto,
                                 @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        dto.setUserId(resolveUserId(dto.getUserId(), currentUserId));
        menuService.update(id, dto);
        return ResultVO.ok();
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id,
                                 @RequestParam(required = false) String userId,
                                 @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        menuService.softDelete(id, resolveUserId(userId, currentUserId));
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
}
