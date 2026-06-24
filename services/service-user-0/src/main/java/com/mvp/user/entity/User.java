package com.mvp.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mvp.user.enums.UserPermission;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户表
 *
 * <p>Entity 是数据库持久化对象，字段和 t_user 表字段一一对应。
 * Controller 层对外收发数据时优先使用 DTO，真正落库时再由 BaseController 把 DTO 转成 Entity。</p>
 *
 * <p>本类上的 MyBatis-Plus 注解负责描述表名、主键策略和字段映射；
 * 这些注解会被 Mapper、Service 以及 BaseController 中的通用 CRUD 间接使用。</p>
 *
 * @TableName t_user
 */
@TableName(value ="t_user")
@Data
public class User implements Serializable {
    /**
     * 用户ID，32位无横杠UUIDv7格式
     *
     * <p>{@code IdType.ASSIGN_UUID} 会让 MyBatis-Plus 在新增数据时调用
     * IdentifierGenerator.nextUUID() 生成主键。项目中的 UuidV7IdentifierGenerator
     * 覆盖了 nextUUID()，因此这里最终生成的是 32 位无横杠 UUIDv7 字符串。</p>
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 手机号
     *
     * <p>数据库字段名是 phone，实体属性名也叫 phone，字段名一致时映射最直观。</p>
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField(value = "email")
    private String email;

    /**
     * BCrypt加密后的密码
     *
     * <p>该字段表示密码哈希值，不表示明文密码。是否对外出现在 DTO 或接口中，应该由业务接口设计决定。</p>
     */
    @TableField(value = "password_hash")
    private String passwordHash;

    /**
     * 用户名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 头像URL
     */
    @TableField(value = "avatar_url")
    private String avatarUrl;

    /**
     * 性别: 0-未知, 1-男, 2-女
     */
    @TableField(value = "gender")
    private Integer gender;

    /**
     * 出生日期
     */
    @TableField(value = "birthday")
    private Date birthday;

    /**
     * 用户权限: ADMIN-管理员, USER-普通用户。
     */
    @TableField(value = "permission")
    private UserPermission permission;

    /**
     * 状态: 0-禁用, 1-正常, 2-注销
     *
     * <p>实体层只保存数据库中的状态值；状态含义的转换、展示文案等更适合放在业务层或前端处理。</p>
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 最后登录时间
     *
     * <p>这类审计字段通常由系统行为维护，而不是由普通用户资料接口手动填写。</p>
     */
    @TableField(value = "last_login_at")
    private Date lastLoginAt;

    /**
     * 创建时间
     *
     * <p>如果后续配置 MyBatis-Plus 自动填充，可以在插入时统一维护该字段。</p>
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     *
     * <p>如果后续配置 MyBatis-Plus 自动填充，可以在更新时统一维护该字段。</p>
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

    /**
     * 软删除时间
     *
     * <p>如果项目要做逻辑删除，可以结合该字段或专门的删除标记字段实现。</p>
     */
    @TableField(value = "deleted_at")
    private Date deletedAt;

    /**
     * Java 序列化版本号。
     *
     * <p>{@code static final} 常量不是数据库字段；这里显式加 {@code @TableField(exist = false)}
     * 是为了清楚表达它不参与 MyBatis-Plus 的表字段映射。</p>
     */
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
