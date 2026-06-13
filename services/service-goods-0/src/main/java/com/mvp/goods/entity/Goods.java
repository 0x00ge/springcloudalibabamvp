package com.mvp.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品表
 *
 * <p>Entity 是数据库持久化对象，字段和 t_goods 表字段一一对应。
 * Controller 层对外收发数据时优先使用 DTO，真正落库时再由 BaseController 把 DTO 转成 Entity。</p>
 *
 * <p>一张表同时承载商品配置、秒杀价、总库存、限购数量和秒杀时间窗口。
 * 注意：精简版没有可用库存列，真实剩余库存完全由 Redis 原子计数维护，
 * 数据库里的 {@code total_stock} 只作为库存初始化的种子和展示用途。</p>
 *
 * @TableName t_goods
 */
@TableName(value = "t_goods")
@Data
public class Goods implements Serializable {

    /**
     * 商品状态：禁用。
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 商品状态：启用。
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 商品ID，32位无横杠UUIDv7格式
     *
     * <p>{@code IdType.ASSIGN_UUID} 会让 MyBatis-Plus 在新增数据时调用
     * IdentifierGenerator.nextUUID() 生成主键。项目中的 UuidV7IdentifierGenerator
     * 覆盖了 nextUUID()，因此这里最终生成的是 32 位无横杠 UUIDv7 字符串。</p>
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 商品名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 秒杀价
     */
    @TableField(value = "seckill_price")
    private BigDecimal seckillPrice;

    /**
     * 总库存
     *
     * <p>仅作为 Redis 库存初始化的种子和展示用途，真实剩余库存以 Redis 原子计数为准。</p>
     */
    @TableField(value = "total_stock")
    private Integer totalStock;

    /**
     * 每人限购数量
     */
    @TableField(value = "limit_per_user")
    private Integer limitPerUser;

    /**
     * 秒杀开始时间
     */
    @TableField(value = "start_time")
    private Date startTime;

    /**
     * 秒杀结束时间
     */
    @TableField(value = "end_time")
    private Date endTime;

    /**
     * 状态: 0-禁用, 1-启用
     *
     * <p>实体层只保存数据库中的状态值；状态含义的转换、展示文案等更适合放在业务层或前端处理。</p>
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Date createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private Date updatedAt;

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
