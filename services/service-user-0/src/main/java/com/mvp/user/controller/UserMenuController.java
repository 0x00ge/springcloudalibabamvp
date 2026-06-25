package com.mvp.user.controller;

import com.mvp.common.vo.ResultVO;
import com.mvp.user.dto.UserMenuDto;
import com.mvp.user.service.UserMenuService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户菜单控制器。
 */
@Validated
@RestController
@RequestMapping("/user/menu")
public class UserMenuController {

    private final UserMenuService userMenuService;

    public UserMenuController(UserMenuService userMenuService) {
        this.userMenuService = userMenuService;
    }

    /**
     * 查询当前登录用户菜单树。
     */
    @GetMapping("/tree")
    public ResultVO<List<UserMenuDto>> tree(@RequestHeader("X-User-Id") @NotBlank(message = "用户ID不能为空") String userId) {
        return ResultVO.ok(userMenuService.treeByUserId(userId));
    }
}
