package com.mvp.order.entity;

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
 * 订单表
 *
 * <p>Entity 是数据库持久化对象，字段和 t_order 表字段一一对应。
 * Controller 层对外收发数据时优先使用 DTO，真正落库时再由业务层把 DTO 转成 Entity。</p>
 *
 * <p>精简版只保留下单最小闭环需要的字段：关联商品、下单用户、购买数量、订单金额和状态。</p>
 *
 * @TableName t_order
 */
@TableName(value = "t_order")
@Data
public class Order implements Serializable {

    /**
     * 订单状态：待支付。
     */
    public static final int STATUS_PENDING_PAY = 0;

    /**
     * 订单状态：已支付。
     */
    public static final int STATUS_PAID = 1;

    /**
     * 订单状态：已取消。
     */
    public static final int STATUS_CANCELED = 2;

    /**
     * 订单ID，32位无横杠UUIDv7格式
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 商品ID
     */
    @TableField(value = "goods_id")
    private String goodsId;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 购买数量
     */
    @TableField(value = "buy_count")
    private Integer buyCount;

    /**
     * 订单金额 = 秒杀价 × 购买数量
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 状态: 0-待支付, 1-已支付, 2-已取消
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
     */
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
