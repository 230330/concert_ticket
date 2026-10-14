package com.concert.annotation;

import java.lang.annotation.*;

/**
 * 演唱会缓存清理注解
 * 标记在管理端写接口上，由 ConcertCacheEvictionAspect 在方法正常返回后清空用户端演唱会相关缓存
 *
 * <p>使用示例：
 * <pre>
 *   &#64;EvictConcertCache
 *   &#64;PutMapping("/{id}")
 *   public Result&lt;Void&gt; update(...) { ... }
 * </pre>
 *
 * @author hzf
 * @date 2026/10/14
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EvictConcertCache {
}
