package com.mvp.model.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * model 模块提供的 MyBatis-Plus 自动配置。
 *
 * <p>业务服务只要依赖 model 模块，就会自动注册分页拦截器，
 * 让 BaseController.page() 使用数据库物理分页。</p>
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
