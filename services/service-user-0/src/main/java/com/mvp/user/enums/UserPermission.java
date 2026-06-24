package com.mvp.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户权限。
 */
@Getter
public enum UserPermission {

    /**
     * 管理员。
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 普通用户。
     */
    USER("USER", "普通用户");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    UserPermission(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
