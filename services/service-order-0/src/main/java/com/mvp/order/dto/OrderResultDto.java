package com.mvp.order.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 秒杀结果 DTO。
 *
 * <p>最小版返回统一状态和必要业务信息，让前端能够知道是排队中、成功还是失败。</p>
 */
@Data
public class OrderResultDto implements Serializable {

    /**
     * 结果状态：排队中。
     */
    public static final int STATUS_QUEUEING = 0;

    /**
     * 结果状态：成功。
     */
    public static final int STATUS_SUCCESS = 1;

    /**
     * 结果状态：失败。
     */
    public static final int STATUS_FAIL = 2;

    /**
     * 结果状态。
     */
    private Integer status;

    /**
     * 请求流水号。
     */
    private String requestNo;

    /**
     * 成功时的订单 ID。
     */
    private String orderId;

    /**
     * 提示信息。
     */
    private String message;

    @Serial
    private static final long serialVersionUID = 1L;
}
