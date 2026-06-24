package com.mvp.user.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户菜单 DTO。
 *
 * <p>字段和 {@code t_user_menu} 基本同名，额外的 {@code children} 用于菜单树接口返回。</p>
 */
@TableName(value = "t_user_menu")
@Data
public class UserMenuDto implements Serializable {

    /**
     * 菜单ID，32位无横杠UUIDv7格式。
     */
    @Size(max = 32, message = "菜单id长度不能超过32")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "菜单id必须是32位无横杠UUIDv7")
    @TableId(value = "id")
    private String id;

    /**
     * 用户ID。
     */
    @NotBlank(message = "用户ID不能为空")
    @Size(max = 32, message = "用户ID长度不能超过32")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "用户ID必须是32位无横杠UUIDv7")
    @TableField(value = "user_id")
    private String userId;

    /**
     * 父菜单ID，NULL表示一级菜单。
     */
    @Size(max = 32, message = "父菜单ID长度不能超过32")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "父菜单ID必须是32位无横杠UUIDv7")
    @TableField(value = "parent_id")
    private String parentId;

    /**
     * 菜单层级。
     */
    @Min(value = 1, message = "菜单层级不能小于1")
    @TableField(value = "level")
    private Integer level;

    /**
     * 排序值，越小越靠前。
     */
    @NotNull(message = "排序值不能为空")
    @Min(value = 0, message = "排序值不能小于0")
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 菜单显示名称。
     */
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50")
    @TableField(value = "title")
    private String title;

    /**
     * 前端路由路径。
     */
    @NotBlank(message = "菜单路径不能为空")
    @Size(max = 200, message = "菜单路径长度不能超过200")
    @TableField(value = "path")
    private String path;

    /**
     * 菜单图标名称。
     */
    @Size(max = 50, message = "菜单图标长度不能超过50")
    @TableField(value = "icon")
    private String icon;

    /**
     * 创建时间。
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

    /**
     * 软删除时间。
     */
    @TableField(value = "deleted_at")
    private Date deletedAt;

    /**
     * 子菜单列表。
     */
    @TableField(exist = false)
    private List<UserMenuDto> children;

    @Serial
    private static final long serialVersionUID = 1L;
}
