package com.mvp.goods.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品表DTO
 *
 * <p>DTO 是 Controller 对外收发的数据对象。前端请求进入 Controller 时，{@code @Valid}
 * 会读取本类字段上的校验注解；Controller 返回数据时，也可以用 DTO 控制返回给前端的字段范围。</p>
 *
 * <p>当前 DTO 字段和 Goods Entity 保持同名，因此 BaseController 可以直接通过
 * BeanUtils 做同名属性拷贝。</p>
 *
 * @TableName t_goods
 */
@TableName(value = "t_goods")
@Data
public class GoodsDto implements Serializable {

    /**
     * 商品ID，32位无横杠UUIDv7格式
     *
     * <p>新增时通常不需要前端传入 id，MyBatis-Plus 会根据实体主键策略生成。
     * 修改时以 URL 路径中的 id 为准，BaseController 会把路径 id 写回 Entity。</p>
     */
    @Size(max = 32, message = "商品id长度不能超过32")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "商品id必须是32位无横杠UUIDv7")
    @TableId(value = "id")
    private String id;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称长度不能超过200")
    @TableField(value = "name")
    private String name;

    /**
     * 秒杀价
     */
    @NotNull(message = "秒杀价不能为空")
    @DecimalMin(value = "0.00", message = "秒杀价不能小于0")
    @TableField(value = "seckill_price")
    private BigDecimal seckillPrice;

    /**
     * 总库存
     */
    @NotNull(message = "总库存不能为空")
    @Min(value = 0, message = "总库存不能小于0")
    @TableField(value = "total_stock")
    private Integer totalStock;

    /**
     * 每人限购数量
     */
    @NotNull(message = "限购数量不能为空")
    @Min(value = 1, message = "限购数量不能小于1")
    @TableField(value = "limit_per_user")
    private Integer limitPerUser;

    /**
     * 秒杀开始时间
     */
    @NotNull(message = "秒杀开始时间不能为空")
    @TableField(value = "start_time")
    private Date startTime;

    /**
     * 秒杀结束时间
     */
    @NotNull(message = "秒杀结束时间不能为空")
    @TableField(value = "end_time")
    private Date endTime;

    /**
     * 状态: 0-禁用, 1-启用
     */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 1, message = "状态值不能大于1")
    @TableField(value = "status")
    private Integer status;

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
     * Java 序列化版本号。
     */
    @Serial
    private static final long serialVersionUID = 1L;
}
