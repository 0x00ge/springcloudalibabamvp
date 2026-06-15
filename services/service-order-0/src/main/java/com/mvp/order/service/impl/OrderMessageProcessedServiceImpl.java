package com.mvp.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mvp.order.entity.OrderMessageProcessed;
import com.mvp.order.mapper.OrderMessageProcessedMapper;
import com.mvp.order.service.OrderMessageProcessedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单事件消息已处理记录服务实现
 */
@Slf4j
@Service
public class OrderMessageProcessedServiceImpl extends ServiceImpl<OrderMessageProcessedMapper, OrderMessageProcessed>
        implements OrderMessageProcessedService {

    @Override
    public boolean isProcessed(String messageId) {
        LambdaQueryWrapper<OrderMessageProcessed> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderMessageProcessed::getMessageId, messageId);
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean markAsProcessed(String messageId, String businessKey) {
        OrderMessageProcessed record = new OrderMessageProcessed();
        record.setMessageId(messageId);
        record.setBusinessKey(businessKey);
        record.setProcessedAt(LocalDateTime.now());

        try {
            return this.save(record);
        } catch (DuplicateKeyException ex) {
            // 唯一索引冲突，说明消息已被其他消费者处理
            log.warn("消息已被标记为已处理 messageId={}", messageId);
            return false;
        }
    }
}
