package com.mvp.model.config;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MyBatis-Plus 分页配置。
 *
 * <p>配置前缀：mybatis-plus.pagination。</p>
 */
@Data
@ConfigurationProperties(prefix = "mybatis-plus.pagination")
public class MybatisPlusPaginationProperties {

    /**
     * 数据库类型，默认按当前项目 MySQL 配置生成分页 SQL。
     */
    private DbType dbType = DbType.MYSQL;

    /**
     * 单页最大数量，避免前端传入过大的 size。
     */
    private Long maxLimit = 500L;

    /**
     * 当前页超过总页数时是否回到第一页。
     */
    private boolean overflow = false;
}
