package com.mvp.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mvp.common.controller.BaseController;
import com.mvp.common.vo.ResultVO;
import com.mvp.user.dto.UserMenuDto;
import com.mvp.user.entity.UserMenu;
import com.mvp.user.service.UserMenuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
     * 根据主键查询用户菜单。
     */
    @Override
    @GetMapping("/{id}")
    public ResultVO<UserMenuDto> getById(@PathVariable String id) {
        return super.getById(id);
    }

    /**
     * 分页查询用户菜单。
     */
    @Override
    @GetMapping("/page")
    public ResultVO<IPage<UserMenuDto>> page(@RequestParam(defaultValue = "1") Long page,
                                             @RequestParam(defaultValue = "10") Long size) {
        return super.page(page, size);
    }

    /**
     * 新增用户菜单。
     */
    @Override
    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody UserMenuDto dto) {
        return super.save(dto);
    }

    /**
     * 更新用户菜单。
     */
    @Override
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id, @Valid @RequestBody UserMenuDto dto) {
        return super.update(id, dto);
    }

    /**
     * 删除用户菜单。
     */
    @Override
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id) {
        return super.delete(id);
    }
}
