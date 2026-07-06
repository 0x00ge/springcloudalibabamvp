package com.mvp.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 菜单表。
 *
 * <p>菜单归属于用户，使用 parent_id 形成树形结构。deleted_at 不为空表示已软删除。</p>
 */
@TableName(value = "t_menu")
@Data
public class Menu implements Serializable {

    /**
     * 菜单ID，32位无横杠UUIDv7格式。
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 父菜单ID，NULL表示一级菜单。
     */
    @TableField(value = "parent_id")
    private String parentId;

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
     * 菜单图标名称。
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 菜单层级，从1开始。
     */
    @TableField(value = "level")
    private Integer level;

    /**
     * 排序值，越小越靠前。
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /**
     * 用户ID。
     */
    @TableField(value = "user_id")
    private String userId;

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
