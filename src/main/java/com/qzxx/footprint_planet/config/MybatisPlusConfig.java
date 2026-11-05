package com.qzxx.footprint_planet.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    /** 单页条数上限（业务语义）；游标分页要多取一条判断 hasMore，所以数据库层放到 51 */
    public static final int MAX_PAGE_SIZE = 50;

    /** 分页插件：配合 MyBatis-Plus 的 selectPage 使用 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大条数：不限制时可一次拉全表。超出部分会被静默截断而不是报错，
        // 避免为了校验再引入一个全局异常分支。
        // 评论/通知用的是游标分页，实现为 new Page<>(1, pageSize + 1)，
        // 多取的那一条只用来判断 hasMore（size > pageSize）。若上限正好等于 50，
        // pageSize=50 时请求的 51 条会被截成 50 条，hasMore 恒为 false，
        // 表现为「滚动加载到第 50 条就再也加载不出来」。
        pagination.setMaxLimit((long) MAX_PAGE_SIZE + 1);
        interceptor.addInnerInterceptor(pagination);
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
