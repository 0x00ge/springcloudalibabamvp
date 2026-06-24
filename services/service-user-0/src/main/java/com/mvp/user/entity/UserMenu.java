package com.mvp.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户菜单表。
 *
 * <p>每条记录表示某个用户可见的一项菜单。一级菜单 {@code parentId} 为空，
 * 子菜单通过 {@code parentId} 关联父菜单。</p>
 */
@TableName(value = "t_user_menu")
@Data
public class UserMenu implements Serializable {

    /**
     * 菜单ID，32位无横杠UUIDv7格式。
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 用户ID。
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 父菜单ID，NULL表示一级菜单。
     */
    @TableField(value = "parent_id")
    private String parentId;

    /**
     * 菜单层级: 1-一级菜单, 2-二级菜单, 依次递增。
     */
    @TableField(value = "level")
    private Integer level;

    /**
     * 排序值，越小越靠前。
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 菜单显示名称。
     */
    @TableField(value = "title")
    private String title;

    /**
     * 前端路由路径。
     */
    @TableField(value = "path")
    private String path;

    /**
     * 菜单图标名称，对应前端 Element Plus 图标。
     */
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

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
