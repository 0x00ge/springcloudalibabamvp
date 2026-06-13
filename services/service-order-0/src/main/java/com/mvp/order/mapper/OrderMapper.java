package com.mvp.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mvp.order.entity.Order;

/**
 * 订单 Mapper。
 *
 * <p>继承 {@link BaseMapper} 后自动拥有 insert、selectById、selectPage、updateById 等基础数据库 CRUD 能力，
 * 下单落库、用户已有订单的防重计数、结果查询时的订单回查都基于它完成。</p>
 *
 * <p>简单单表操作靠继承即可，无需在 XML 写 SQL；{@code mapper/OrderMapper.xml} 目前只维护
 * {@code BaseResultMap} 和 {@code Base_Column_List}，供后续编写复杂 SQL 时复用列定义。</p>
 *
 * @author zhongtao
 * @description 针对表【t_order(订单表)】的数据库操作Mapper
 * @Entity com.mvp.order.entity.Order
 */
public interface OrderMapper extends BaseMapper<Order> {
}
