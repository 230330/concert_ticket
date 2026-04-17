package com.concert.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * @description: MyBatis-Plus 配置类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件（设置全局分页上限，防止恶意大查询拖垮数据库）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 全局分页上限：即使 Controller/Service 未校验，框架也会自动截断
        paginationInterceptor.setMaxLimit(100L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

    /**
     * MyBatis-Plus 自动填充处理器（字段审计）
     *
     * 优化说明：
     * 1. 使用 fillStrategy 替代 strict 模式，避免字段名不匹配导致的异常。
     * 2. 使用字段名常量避免硬编码拼写错误。
     * 3. 插入时保证 createTime 和 updateTime 时间完全一致。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            // 建议定义常量，与实体类属性名保持一致
            private static final String CREATE_TIME = "createTime";
            private static final String UPDATE_TIME = "updateTime";

            @Override
            public void insertFill(MetaObject metaObject) {
                // 方案一：使用非严格模式 + 统一时间对象（推荐）
                LocalDateTime now = LocalDateTime.now();
                this.fillStrategy(metaObject, CREATE_TIME, now);
                this.fillStrategy(metaObject, UPDATE_TIME, now);

                // 方案二：如果坚持使用 strict 模式，记得用同一个时间变量
                // this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
                // this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 更新时只更新修改时间
                this.fillStrategy(metaObject, UPDATE_TIME, LocalDateTime.now());
            }
        };
    }
}
