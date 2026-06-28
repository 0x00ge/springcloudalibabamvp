package com.mvp.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mvp.common.controller.BaseController;
import com.mvp.user.enums.UserPermission;
import com.mvp.user.dto.UserDto;
import com.mvp.user.entity.User;
import com.mvp.common.vo.ResultVO;
import com.mvp.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.BeanUtils;
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

    @GetMapping("/page")
    public ResultVO<IPage<UserDto>> page(@RequestParam(defaultValue = "1") Long page,
                                         @RequestParam(defaultValue = "10") Long size,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) String phone,
                                         @RequestParam(required = false) String email,
                                         @RequestParam(required = false) UserPermission permission,
                                         @RequestParam(required = false) Integer status) {
        Page<User> queryPage = new Page<>(page, size);
        IPage<UserDto> result = userService.page(queryPage, Wrappers.<User>lambdaQuery()
                .like(StringUtils.hasText(name), User::getName, name)
                .like(StringUtils.hasText(phone), User::getPhone, phone)
                .like(StringUtils.hasText(email), User::getEmail, email)
                .eq(permission != null, User::getPermission, permission)
                .eq(status != null, User::getStatus, status)
                .isNull(User::getDeletedAt)
                .orderByDesc(User::getCreatedAt))
                .convert(this::toDto);
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

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
