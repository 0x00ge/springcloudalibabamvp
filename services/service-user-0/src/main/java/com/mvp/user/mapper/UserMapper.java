package com.mvp.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mvp.user.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
 * @Entity com.mvp.user.entity.User
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 全表查询（resultType 自动映射）。
     */
    List<User> userList();

    /**
     * 全表查询（resultMap = BaseResultMap）。
     */
    List<User> userMapList();

    /**
     * 可选条件查询：phone / name / status。
     */
    List<User> findUsers(@Param("phone") String phone,
                        @Param("name") String name,
                        @Param("status") Integer status);

    /**
     * 按关键字类型分支查询：type = phone | name | 其他。
     */
    List<User> findByKeyword(@Param("phone") String phone,
                        @Param("name") String name,
                        @Param("status") Integer status);

    /**
     * 动态更新用户（只更新非 null 字段）。
     */
    int updateUser(User user);

    /**
     * 按 id 列表批量查询。
     */
    List<User> listByIds(@Param("ids") List<String> ids);

    /**
     * 按姓名模糊查询（bind 生成 nameLike）。
     */
    List<User> searchByName(@Param("name") String name);

    /**
     * 分页列表：status 过滤 + 动态排序 + limit。
     *
     * <p>{@code orderBy} 使用 ${} 拼接，调用方必须做列名白名单校验。</p>
     */
    List<User> userListPage(@Param("status") Integer status,
                            @Param("orderBy") String orderBy,
                            @Param("offset") Integer offset,
                            @Param("limit") Integer limit);
}
