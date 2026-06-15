package com.mvp.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mvp.order.entity.OrderMessageProcessed;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单事件消息已处理记录 Mapper
 *
 * <p>提供消息去重表的数据访问接口，包括：
 * <ul>
 *   <li>基础 CRUD 操作（继承自 BaseMapper）</li>
 *   <li>消息ID查询（幂等性检查）</li>
 *   <li>业务键查询</li>
 *   <li>批量插入（高并发优化）</li>
 *   <li>历史数据清理</li>
 * </ul>
 */
public interface OrderMessageProcessedMapper extends BaseMapper<OrderMessageProcessed> {
}
