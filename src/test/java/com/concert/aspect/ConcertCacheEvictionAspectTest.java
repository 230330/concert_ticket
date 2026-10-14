package com.concert.aspect;

import com.concert.config.CacheConfig;
import com.concert.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ConcertCacheEvictionAspect 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ConcertCacheEvictionAspectTest {

    /** 切面负责清理的全部缓存区域 */
    private static final String[] ALL_CACHES = {
            CacheConfig.CACHE_CONCERT_LIST,
            CacheConfig.CACHE_CONCERT_DETAIL,
            CacheConfig.CACHE_HOT_CONCERT,
            CacheConfig.CACHE_SHOW_INFO
    };

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Mock
    private Cache cache;

    @InjectMocks
    private ConcertCacheEvictionAspect aspect;

    /**
     * 仅需要触发清理的用例才需要签名信息
     */
    private void stubEvictContext() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AdminShowController.update(..)");
    }

    @Nested
    @DisplayName("写入成功")
    class WriteSucceeded {

        @Test
        @DisplayName("方法正常返回后清理全部四个缓存区域")
        void testAround_EvictsAllCaches() throws Throwable {
            // Given
            stubEvictContext();
            when(joinPoint.proceed()).thenReturn("ok");
            for (String cacheName : ALL_CACHES) {
                when(cacheManager.getCache(cacheName)).thenReturn(cache);
            }

            // When
            Object result = aspect.around(joinPoint);

            // Then
            assertEquals("ok", result);
            for (String cacheName : ALL_CACHES) {
                verify(cacheManager).getCache(cacheName);
            }
            verify(cache, times(ALL_CACHES.length)).clear();
        }

        @Test
        @DisplayName("单个区域清理失败时继续清理其余区域且不向上抛出")
        void testAround_OneCacheFailure_ContinuesAndSwallows() throws Throwable {
            // Given - 模拟 Redis 不可用导致首个区域清理失败
            stubEvictContext();
            when(joinPoint.proceed()).thenReturn("ok");
            when(cacheManager.getCache(CacheConfig.CACHE_CONCERT_LIST))
                    .thenThrow(new RedisConnectionFailureException("redis down"));
            when(cacheManager.getCache(CacheConfig.CACHE_CONCERT_DETAIL)).thenReturn(cache);
            when(cacheManager.getCache(CacheConfig.CACHE_HOT_CONCERT)).thenReturn(cache);
            when(cacheManager.getCache(CacheConfig.CACHE_SHOW_INFO)).thenReturn(cache);

            // When & Then
            assertDoesNotThrow(() -> aspect.around(joinPoint));
            verify(cache, times(ALL_CACHES.length - 1)).clear();
        }

        @Test
        @DisplayName("缓存区域未注册时跳过清理且不向上抛出")
        void testAround_UnregisteredCache_Skips() throws Throwable {
            // Given - getCache 未打桩，默认返回 null
            stubEvictContext();
            when(joinPoint.proceed()).thenReturn("ok");

            // When & Then
            assertDoesNotThrow(() -> aspect.around(joinPoint));
        }
    }

    @Nested
    @DisplayName("写入失败")
    class WriteFailed {

        @Test
        @DisplayName("目标方法抛出异常时不清理缓存")
        void testAround_MethodThrows_SkipsEviction() throws Throwable {
            // Given
            when(joinPoint.proceed()).thenThrow(new BusinessException("票档与场次不匹配"));

            // When & Then
            assertThrows(BusinessException.class, () -> aspect.around(joinPoint));
            verifyNoInteractions(cacheManager);
        }
    }
}
