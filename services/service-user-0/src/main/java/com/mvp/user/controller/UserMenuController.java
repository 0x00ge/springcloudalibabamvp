package com.mvp.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mvp.common.controller.BaseController;
import com.mvp.common.vo.ResultVO;
import com.mvp.user.dto.UserMenuDto;
import com.mvp.user.entity.UserMenu;
import com.mvp.user.service.UserMenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

import java.io.Serializable;
import java.util.List;

/**
 * 用户菜单控制器。
 */
@Validated
@RestController
@RequestMapping("/user/menu")
public class UserMenuController extends BaseController<UserMenu, UserMenuDto> {

    private final UserMenuService userMenuService;

    public UserMenuController(UserMenuService userMenuService) {
        super(userMenuService);
        this.userMenuService = userMenuService;
    }

    /**
     * 查询当前登录用户菜单树。
     */
    @GetMapping("/tree")
    public ResultVO<List<UserMenuDto>> tree(@RequestHeader("X-User-Id") @NotBlank(message = "用户ID不能为空") String userId) {
        return ResultVO.ok(userMenuService.treeByUserId(userId));
    }

    /**
     * 管理端查询菜单树。
     */
    @GetMapping("/manage/tree")
    public ResultVO<List<UserMenuDto>> manageTree(@RequestHeader("X-User-Id") @NotBlank(message = "用户ID不能为空") String operatorUserId,
                                                  @RequestParam(required = false) String userId) {
        return ResultVO.ok(userMenuService.manageTree(operatorUserId, userId));
    }

    /**
     * 根据主键查询用户菜单。
     */
    @Override
    @GetMapping("/{id}")
    public ResultVO<UserMenuDto> getById(@PathVariable String id) {
        userMenuService.manageTree(currentUserId(), currentUserId());
        return super.getById(id);
    }

    /**
     * 新增用户菜单。
     */
    @Override
    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody UserMenuDto dto) {
        return ResultVO.ok(userMenuService.createMenu(currentUserId(), dto));
    }

    /**
     * 更新用户菜单。
     */
    @Override
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id, @Valid @RequestBody UserMenuDto dto) {
        userMenuService.updateMenu(currentUserId(), id, dto);
        return ResultVO.ok();
    }

    /**
     * 删除用户菜单。
     */
    @Override
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id) {
        userMenuService.deleteMenu(currentUserId(), id);
        return ResultVO.ok();
    }

    /**
     * 分页接口保留父类能力，但进入管理接口前也要做管理员校验。
     */
    @Override
    @GetMapping("/page")
    public ResultVO<IPage<UserMenuDto>> page(@RequestParam(defaultValue = "1") Long page,
                                             @RequestParam(defaultValue = "10") Long size) {
        userMenuService.manageTree(currentUserId(), currentUserId());
        return super.page(page, size);
    }

    private String currentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        HttpServletRequest request = attributes.getRequest();
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        return userId;
    }
}
