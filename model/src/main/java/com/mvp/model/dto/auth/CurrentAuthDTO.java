package com.mvp.model.dto.auth;

import lombok.Data;

/**
 * 当前登录用户信息。
 */
@Data
public class CurrentAuthDTO {

    /**
     * 用户ID。
     */
    private String id;

    /**
     * 用户名称。
     */
    private String name;

    /**
     * 手机号。
     */
    private String phone;
}
