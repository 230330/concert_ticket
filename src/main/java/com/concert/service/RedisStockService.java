package com.concert.service;

/**
 * @description: Redis库存预扣减服务接口
 * 使用Redis Lua脚本实现原子性库存预扣/回滚，防止超卖
 * @author: hzf
 * @date: 2026-06-04
 */
public interface RedisStockService {

    /**
     * 预扣库存（原子操作）
     *
     * @param ticketTypeId 票档ID
     * @param quantity     扣减数量
     * @return 是否预扣成功
     */
    boolean preDeductStock(Long ticketTypeId, int quantity);

    /**
     * 回滚预扣库存（原子操作）
     *
     * @param ticketTypeId 票档ID
     * @param quantity     回滚数量
     */
    void rollbackStock(Long ticketTypeId, int quantity);

    /**
     * 获取Redis中的库存余量
     *
     * @param ticketTypeId 票档ID
     * @return 库存余量，如果Redis中无缓存返回-1
     */
    int getAvailableStock(Long ticketTypeId);

    /**
     * 从数据库加载指定票档的库存到Redis
     *
     * @param ticketTypeId 票档ID
     */
    void loadStockToRedis(Long ticketTypeId);

    /**
     * 从数据库加载所有票档库存到Redis（系统启动时调用）
     */
    void loadAllStockToRedis();

    /**
     * 初始化库存缓存（如果Redis中不存在则从DB加载）
     *
     * @param ticketTypeId 票档ID
     */
    void initStockIfNeeded(Long ticketTypeId);
}
