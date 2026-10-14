package com.concert.aspect;

import com.concert.config.CacheConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * 演唱会缓存清理切面
 * 处理 @EvictConcertCache 注解：管理端写入成功后清空用户端演唱会相关缓存
 *
 * <p>写入失败的请求（方法抛出异常，事务回滚）不会触发清理；
 * 清理过程本身失败只记录告警，不影响写操作结果，避免 Redis 不可用时连管理端编辑都不可用
 *
 * @author hzf
 * @date 2026/10/14
 */
@Aspect
@Component
public class ConcertCacheEvictionAspect {

    private static final Logger logger = LoggerFactory.getLogger(ConcertCacheEvictionAspect.class);

    /**
     * 受管理端写入影响的用户端缓存区域
     * 这四个区域的响应都同时依赖演唱会、场次、场馆、艺人、票档五类实体，无法按实体做细粒度失效
     */
    private static final String[] AFFECTED_CACHES = {
            CacheConfig.CACHE_CONCERT_LIST,
            CacheConfig.CACHE_CONCERT_DETAIL,
            CacheConfig.CACHE_HOT_CONCERT,
            CacheConfig.CACHE_SHOW_INFO
    };

    @Resource
    private CacheManager cacheManager;

    @Around("@annotation(com.concert.annotation.EvictConcertCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        // 方法正常返回后才清理缓存，写入失败时保持缓存不动
        evictAffectedCaches(joinPoint.getSignature().toShortString());

        return result;
    }

    /**
     * 逐个清空受影响的缓存区域
     * 单个区域清理失败不影响其余区域，也不向调用方抛出
     */
    private void evictAffectedCaches(String method) {
        for (String cacheName : AFFECTED_CACHES) {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache == null) {
                    logger.warn("缓存区域未注册，跳过清理：cache={}, method={}", cacheName, method);
                    continue;
                }
                cache.clear();
            } catch (Exception e) {
                logger.warn("清理缓存区域失败（不影响业务）：cache={}, method={}", cacheName, method, e);
            }
        }

        logger.info("管理端写入完成，已清理演唱会相关缓存：method={}, caches={}", method,
                Arrays.toString(AFFECTED_CACHES));
    }
}
