package com.mvp.user.mapper;

import com.mvp.model.entity.user.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* 用户 Mapper。
*
* <p>继承 {@link BaseMapper} 后，UserMapper 自动拥有 insert、deleteById、updateById、
* selectById、selectPage 等基础数据库操作。ServiceImpl 会基于这个 Mapper 提供更上层的
* Service CRUD 能力。</p>
*
* <p>如果后续有复杂 SQL，可以继续在这里声明 Mapper 方法，并在 mapper/UserMapper.xml
* 中编写对应 SQL。</p>
*
* @author zhongtao
* @description 针对表【t_user(用户表)】的数据库操作Mapper
* @createDate 2026-06-06 17:18:05
* @Entity com.mvp.model.entity.user.User
*/
public interface UserMapper extends BaseMapper<User> {

}




