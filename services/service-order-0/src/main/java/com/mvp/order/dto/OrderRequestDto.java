package com.mvp.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 下单请求 DTO。
 *
 * <p>精简版商品表自带时间窗口，不再有独立活动维度，所以下单只需要指定商品 ID 和购买数量。</p>
 */
@Data
public class OrderRequestDto implements Serializable {

    /**
     * 商品ID，32位无横杠UUIDv7格式
     */
    @NotBlank(message = "商品ID不能为空")
    @Pattern(regexp = "^[0-9a-fA-F]{32}$", message = "商品ID必须是32位无横杠UUIDv7")
    private String goodsId;

    /**
     * 购买数量，默认 1，上限受商品限购约束。
     */
    @Min(value = 1, message = "购买数量不能小于1")
    @Max(value = 100, message = "购买数量不能大于100")
    private Integer buyCount = 1;

    @Serial
    private static final long serialVersionUID = 1L;
}
