package com.mvp.user.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 菜单 DTO。
 */
@TableName(value = "t_menu")
@Data
public class MenuDto implements Serializable {

    @Size(max = 32, message = "菜单id长度不能超过32")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "菜单id必须是32位无横杠UUIDv7")
    @TableId(value = "id")
    private String id;

    @Size(max = 32, message = "父菜单id长度不能超过32")
    @Pattern(regexp = "^$|^[0-9a-fA-F]{32}$", message = "父菜单id必须是32位无横杠UUIDv7")
    @TableField(value = "parent_id")
    private String parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50")
    @TableField(value = "title")
    private String title;

    @NotBlank(message = "菜单路径不能为空")
    @Size(max = 200, message = "菜单路径长度不能超过200")
    @Pattern(regexp = "^/.*", message = "菜单路径必须以/开头")
    @TableField(value = "path")
    private String path;

    @Size(max = 50, message = "菜单图标长度不能超过50")
    @TableField(value = "icon")
    private String icon;

    @Min(value = 1, message = "菜单层级不能小于1")
    @TableField(value = "level")
    private Integer level;

    @Min(value = 0, message = "排序值不能小于0")
    @TableField(value = "sort_order")
    private Integer sortOrder;

    @Size(max = 32, message = "用户id长度不能超过32")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "用户id必须是32位无横杠UUIDv7")
    @TableField(value = "user_id")
    private String userId;

    @TableField(value = "created_at")
    private Date createdAt;

    @TableField(value = "updated_at")
    private Date updatedAt;

    @TableField(value = "deleted_at")
    private Date deletedAt;

    @TableField(exist = false)
    private List<MenuDto> children = new ArrayList<>();

    @Serial
    private static final long serialVersionUID = 1L;
}
