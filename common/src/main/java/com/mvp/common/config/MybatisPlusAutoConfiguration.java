package com.mvp.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * common 模块提供的 MyBatis-Plus 自动配置。
 *
 * <p>业务服务只要依赖 common 模块，就会自动注册分页拦截器，
 * 让 BaseController.page() 使用数据库物理分页。</p>
 *
 * <p>{@code @ConditionalOnClass(MybatisPlusInterceptor.class)} 保证只有在引入了
 * mybatis-plus 扩展（即真正需要做数据访问的业务服务）时才生效。网关只依赖 common 的
 * mybatis-plus-core，不含 MybatisPlusInterceptor，因此本自动配置在网关侧不会激活，
 * 也就不会触发任何数据源相关装配。</p>
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties(MybatisPlusPaginationProperties.class)
public class MybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusPaginationProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInnerInterceptor =
                new PaginationInnerInterceptor(properties.getDbType());
        paginationInnerInterceptor.setMaxLimit(properties.getMaxLimit());
        paginationInnerInterceptor.setOverflow(properties.isOverflow());

        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
