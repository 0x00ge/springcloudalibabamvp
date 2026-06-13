package com.mvp.goods.mapper;

import com.mvp.goods.entity.Goods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 商品 Mapper。
 *
 * <p>继承 {@link BaseMapper} 后，GoodsMapper 自动拥有 insert、deleteById、updateById、
 * selectById、selectPage 等基础数据库操作。ServiceImpl 会基于这个 Mapper 提供更上层的
 * Service CRUD 能力。</p>
 *
 * <p>简单单表操作完全靠继承得到，无需在 {@code mapper/GoodsMapper.xml} 里写 SQL；
 * XML 中目前只维护 {@code BaseResultMap} 和 {@code Base_Column_List}，供后续编写复杂 SQL 时复用列定义。
 * 如果后续有联表、统计等复杂查询，可以在本接口声明方法，并在 XML 中编写对应 SQL。</p>
 *
 * @author zhongtao
 * @description 针对表【t_goods(商品表)】的数据库操作Mapper
 * @Entity com.mvp.goods.entity.Goods
 */
public interface GoodsMapper extends BaseMapper<Goods> {

}
