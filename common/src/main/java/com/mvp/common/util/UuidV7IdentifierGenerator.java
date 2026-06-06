package com.mvp.common.util;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 主键生成器。
 *
 * <p>当实体使用 IdType.ASSIGN_UUID 时，MyBatis-Plus 会调用 nextUUID()。
 * 这里统一返回 32 位无横杠 UUIDv7，使用户主键既全局唯一，又大致按创建时间递增。</p>
 *
 * <p>该类加了 {@link Component}，Spring 启动时会把它注册成 Bean。
 * MyBatis-Plus 检测到自定义 IdentifierGenerator Bean 后，会优先使用它生成主键。</p>
 */
@Component
public class UuidV7IdentifierGenerator implements IdentifierGenerator {

    /**
     * 数字型主键仍然交给 MyBatis-Plus 默认生成器处理。
     *
     * <p>这样项目里如果有实体使用 {@code IdType.ASSIGN_ID} 或雪花数字 id，不会被 UUIDv7 逻辑影响。</p>
     */
    private final DefaultIdentifierGenerator defaultIdentifierGenerator = DefaultIdentifierGenerator.getInstance();

    /**
     * 生成数字型主键。
     *
     * <p>本项目当前用户主键是字符串 UUID，所以主要使用的是 {@link #nextUUID(Object)}。
     * 保留默认实现是为了兼容未来可能出现的 Long 类型主键实体。</p>
     */
    @Override
    public Number nextId(Object entity) {
        return defaultIdentifierGenerator.nextId(entity);
    }

    /**
     * 生成字符串型主键。
     *
     * <p>实体字段使用 {@code @TableId(type = IdType.ASSIGN_UUID)} 时，会走到这个方法。
     * 因此 User.id 虽然声明的是 ASSIGN_UUID，最终得到的不是普通 UUIDv4，而是这里返回的无横杠 UUIDv7。</p>
     */
    @Override
    public String nextUUID(Object entity) {
        return UuidV7Util.generate();
    }
}
