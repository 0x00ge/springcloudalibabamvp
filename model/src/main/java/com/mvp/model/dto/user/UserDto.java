package com.mvp.model.dto.user;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户表DTO
 *
 * <p>DTO 是 Controller 对外收发的数据对象。前端请求进入 Controller 时，{@code @Valid}
 * 会读取本类字段上的校验注解；Controller 返回数据时，也可以用 DTO 控制返回给前端的字段范围。</p>
 *
 * <p>当前 DTO 字段和 User Entity 基本保持同名，因此 BaseController 可以直接通过
 * BeanUtils 做同名属性拷贝。如果以后 DTO 和 Entity 字段不一致，需要在具体 Controller
 * 中重写 dto2Entity 或 entity2Dto。</p>
 *
 * @TableName t_user
 */
@TableName(value ="t_user")
@Data
public class UserDto implements Serializable {

    /**
     * 用户ID，32位无横杠UUIDv7格式
     *
     * <p>新增时通常不需要前端传入 id，MyBatis-Plus 会根据实体主键策略生成。
     * 修改时以 URL 路径中的 id 为准，BaseController 会把路径 id 写回 Entity。</p>
     */
    @Size(max = 32, message = "用户id长度不能超过32")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "用户id必须是32位无横杠UUIDv7")
    @TableId(value = "id")
    private String id;

    /**
     * 手机号
     *
     * <p>手机号是当前 DTO 的必填字段。{@code @NotBlank} 校验非空白字符串，
     * {@code @Pattern} 校验大陆手机号格式。</p>
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @TableField(value = "phone")
    private String phone;

    /**
     * 邮箱
     *
     * <p>邮箱不是必填字段；如果前端传了值，则必须符合邮箱格式，并且长度不能超过 100。</p>
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    @TableField(value = "email")
    private String email;

    /**
     * BCrypt加密后的密码
     *
     * <p>这里保存的是已经加密后的密码哈希，不应该保存明文密码。
     * {@code @Size(max = 100)} 用来限制数据库字段长度范围内的数据。</p>
     */
    @NotBlank(message = "密码哈希不能为空")
    @Size(max = 100, message = "密码哈希长度不能超过100")
    @TableField(value = "password_hash")
    private String passwordHash;

    /**
     * 用户名称
     *
     * <p>用户名称对外展示频率较高，因此在 DTO 层限制为必填且最多 50 个字符。</p>
     */
    @NotBlank(message = "用户名称不能为空")
    @Size(max = 50, message = "用户名称长度不能超过50")
    @TableField(value = "name")
    private String name;

    /**
     * 头像URL
     *
     * <p>头像地址不是必填字段；限制最大长度可以避免过长 URL 或异常输入直接进入数据库。</p>
     */
    @Size(max = 500, message = "头像URL长度不能超过500")
    @TableField(value = "avatar_url")
    private String avatarUrl;

    /**
     * 性别: 0-未知, 1-男, 2-女
     *
     * <p>这里用数字枚举表示，DTO 层用 {@code @Min} 和 {@code @Max} 保证只能传入约定范围内的值。</p>
     */
    @Min(value = 0, message = "性别值不能小于0")
    @Max(value = 2, message = "性别值不能大于2")
    @TableField(value = "gender")
    private Integer gender;

    /**
     * 出生日期
     *
     * <p>{@code @PastOrPresent} 表示出生日期不能晚于当前时间，避免出现未来日期。</p>
     */
    @PastOrPresent(message = "出生日期不能晚于当前时间")
    @TableField(value = "birthday")
    private Date birthday;

    /**
     * 状态: 0-禁用, 1-正常, 2-注销
     *
     * <p>状态同样使用数字枚举，DTO 层限制为 0 到 2，防止出现未定义状态。</p>
     */
    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 2, message = "状态值不能大于2")
    @TableField(value = "status")
    private Integer status;

    /**
     * 最后登录时间
     *
     * <p>这类字段通常由系统在登录流程或审计流程中维护，普通新增、修改接口是否允许前端传入，
     * 需要根据业务规则决定。</p>
     */
    @TableField(value = "last_login_at")
    private Date lastLoginAt;

    /**
     * 创建时间
     *
     * <p>创建时间通常由数据库默认值或 MyBatis-Plus 自动填充，不建议由前端手动维护。</p>
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     *
     * <p>更新时间通常在修改数据时自动维护，用来记录最后一次业务更新发生的时间。</p>
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

    /**
     * 软删除时间
     *
     * <p>如果项目采用软删除，可以用该字段记录删除发生时间；如果没有配置软删除，它只是普通字段。</p>
     */
    @TableField(value = "deleted_at")
    private Date deletedAt;

    /**
     * Java 序列化版本号。
     *
     * <p>DTO 实现了 {@link Serializable} 时建议显式声明该字段，避免类结构变化后出现默认版本号不稳定的问题。
     * 这是静态常量，不属于接口请求字段。</p>
     */
    @Serial
    private static final long serialVersionUID = 1L;
}
