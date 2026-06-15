package com.mvp.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mvp.order.entity.OrderMessageProcessed;

/**
 * 订单事件消息已处理记录服务接口
 */
public interface OrderMessageProcessedService extends IService<OrderMessageProcessed> {

    /**
     * 检查消息是否已处理
     *
     * @param messageId 消息 ID
     * @return true=已处理，false=未处理
     */
    boolean isProcessed(String messageId);

    /**
     * 标记消息为已处理
     *
     * @param messageId   消息 ID
     * @param businessKey 业务唯一键
     * @return true=标记成功，false=标记失败（可能已存在）
     */
    boolean markAsProcessed(String messageId, String businessKey);
}
