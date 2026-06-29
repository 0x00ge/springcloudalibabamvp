package com.mvp.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mvp.common.controller.BaseController;
import com.mvp.common.vo.ResultVO;
import com.mvp.user.dto.UserDto;
import com.mvp.user.entity.User;
import com.mvp.user.enums.UserPermission;
import com.mvp.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * 用户控制器
 *
 */
@RestController
@RequestMapping("/user")
public class UserController extends BaseController<User, UserDto> {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserController(UserService userService) {
        super(userService);
        this.userService = userService;
    }

    @Override
    @GetMapping("/{id}")
    public ResultVO<UserDto> getById(@PathVariable String id) {
        return super.getById(id);
    }

    @Override
    @GetMapping("/page")
    public ResultVO<IPage<UserDto>> page(@RequestParam(defaultValue = "1") Long page,
                                         @RequestParam(defaultValue = "10") Long size) {
        HttpServletRequest request = currentRequest();
        String name = request.getParameter("name");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");
        UserPermission permission = parsePermission(request.getParameter("permission"));
        Integer status = parseStatus(request.getParameter("status"));

        Page<User> queryPage = new Page<>(page, size);
        IPage<UserDto> result = userService.page(queryPage, Wrappers.<User>lambdaQuery()
                .like(StringUtils.hasText(name), User::getName, name)
                .like(StringUtils.hasText(phone), User::getPhone, phone)
                .like(StringUtils.hasText(email), User::getEmail, email)
                .eq(permission != null, User::getPermission, permission)
                .eq(status != null, User::getStatus, status)
                .isNull(User::getDeletedAt)
                .orderByAsc(User::getCreatedAt))
                .convert(this::entity2Dto);
        return ResultVO.ok(result);
    }

    @Override
    @PostMapping
    public ResultVO<Serializable> save(@Valid @RequestBody UserDto dto) {
        encodePlainPassword(dto);
        return super.save(dto);
    }

    @Override
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable String id, @Valid @RequestBody UserDto dto) {
        encodePlainPassword(dto);
        return super.update(id, dto);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable String id) {
        return super.delete(id);
    }



    @GetMapping("/test")
    public ResultVO<String> getTest() {
        return ResultVO.ok(userService.getTest());
    }

    private void encodePlainPassword(UserDto dto) {
        String passwordHash = dto.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.startsWith("$2")) {
            return;
        }

        dto.setPasswordHash(passwordEncoder.encode(passwordHash));
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private UserPermission parsePermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            return null;
        }

        return UserPermission.valueOf(permission);
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }

        return Integer.valueOf(status);
    }
}
