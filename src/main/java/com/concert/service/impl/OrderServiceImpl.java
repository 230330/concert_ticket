package com.concert.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.enums.OrderStatus;
import com.concert.enums.ShowStatus;
import com.concert.exception.BusinessException;
import com.concert.exception.ForbiddenException;
import com.concert.exception.NotFoundException;
import com.concert.mapper.OrderMapper;
import com.concert.mapper.OrderSeatMapper;
import com.concert.mapper.TicketTypeMapper;
import com.concert.service.*;
import com.concert.utils.SecureRandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 订单服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    /**
     * 订单过期时间（分钟）
     */
    @Value("${concert.order.expire-minutes:15}")
    private int orderExpireMinutes;

    /**
     * 单笔订单最大购票数
     */
    @Value("${concert.order.max-tickets-per-order:4}")
    private int maxTicketsPerOrder;

    /**
     * 退款截止时间（演出前多少小时可退）
     */
    @Value("${concert.order.refund-before-hours:48}")
    private int refundBeforeHours;

    @Resource
    private ShowService showService;

    @Resource
    private ConcertService concertService;

    @Resource
    private VenueService venueService;

    @Resource
    private TicketTypeService ticketTypeService;

    @Resource
    private SeatService seatService;

    @Resource
    private SeatAreaService seatAreaService;

    @Resource
    private OrderSeatService orderSeatService;

    @Resource
    private OrderSeatMapper orderSeatMapper;

    @Resource
    private TicketTypeMapper ticketTypeMapper;

    @Resource
    private SmsVerificationCodeService smsVerificationCodeService;

    @Resource
    private UserService userService;

    @Resource
    private NotificationService notificationService;
    /**
     * 创建订单
     *
     * @param userId 用户ID
     * @param request 创建订单请求
     * @return 订单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)// 开启事务，确保数据一致性
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. 获取场次信息
        Show show = showService.getById(request.getShowId());
        if (show == null) {
            throw new NotFoundException("场次不存在");
        }

        // 检查场次状态（售票中）
        if (show.getStatus() != ShowStatus.ON_SALE) {
            throw new BusinessException("该场次暂未开放购票");
        }

        // 2. 获取票档信息
        TicketType ticketType = ticketTypeService.getById(request.getTicketTypeId());
        if (ticketType == null) {
            throw new NotFoundException("票档不存在");
        }

        // 检查票档是否属于该场次
        if (!ticketType.getShowId().equals(request.getShowId())) {
            throw new BusinessException("票档与场次不匹配");
        }

        // 3. 检查座位数量
        List<Long> seatIds = request.getSeatIds();
        if (seatIds.size() > maxTicketsPerOrder) {
            throw new BusinessException("单笔订单最多购买" + maxTicketsPerOrder + "张票");
        }

        // 检查库存
        if (ticketType.getAvailableStock() < seatIds.size()) {
            throw new BusinessException("库存不足");
        }

        // 4. 验证座位是否存在且属于该票档对应的区域
        List<Seat> seats = seatService.listByIds(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new NotFoundException("部分座位不存在");
        }

        for (Seat seat : seats) {
            if (!seat.getAreaId().equals(ticketType.getAreaId())) {
                throw new BusinessException("座位与票档区域不匹配");
            }
        }

        // 5. 计算总价
        BigDecimal totalAmount = ticketType.getPrice().multiply(new BigDecimal(seatIds.size()));

        // 6. 生成订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setShowId(request.getShowId());
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING); // 待支付
        order.setExpireTime(LocalDateTime.now().plusMinutes(orderExpireMinutes));

        this.save(order);

        // 7. 批量插入订单座位（利用唯一索引防止并发）
        try {
            List<OrderSeat> orderSeats = new ArrayList<>();
            for (Long seatId : seatIds) {
                OrderSeat orderSeat = new OrderSeat();
                orderSeat.setOrderId(order.getId());
                orderSeat.setShowId(request.getShowId());
                orderSeat.setSeatId(seatId);
                orderSeat.setTicketTypeId(request.getTicketTypeId());
                orderSeat.setPrice(ticketType.getPrice());
                orderSeats.add(orderSeat);
            }
            orderSeatService.saveBatch(orderSeats);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("座位已被占用，请重新选择");
        }

        // 8. 更新票档库存
        LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TicketType::getId, ticketType.getId())
                .ge(TicketType::getAvailableStock, seatIds.size())
                .setSql("available_stock = available_stock - " + seatIds.size());
        boolean updated = ticketTypeService.update(updateWrapper);
        if (!updated) {
            throw new BusinessException("库存不足");
        }

        logger.info("订单创建成功，订单号：{}，用户ID：{}，座位数：{}", order.getOrderNo(), userId, seatIds.size());

        // 9. 返回订单信息
        return getOrderDetail(order.getId());
    }

    /**
     * 支付订单
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse payOrder(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        // 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此订单");
        }

        // 2. 校验订单状态
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 校验是否过期
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException("订单已过期，请重新下单");
        }

        // 3. 更新订单状态
        order.setStatus(OrderStatus.PAID); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setPickupCode(SecureRandomUtil.generateAlphanumericCode(8)); // 生成取票码
        this.updateById(order);

        // 异步发送短信（事务提交后执行）
        String userPhone = userService.getById(userId).getPhone();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.sendPickupCodeSms(userPhone, order.getPickupCode());
            }
        });

        logger.info("订单支付成功，订单号：{}，取票码：{}", order.getOrderNo(), order.getPickupCode());
        return getOrderDetail(orderId);
    }

    /**
     * 取消订单
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        // 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此订单");
        }

        // 2. 校验订单状态（只有待支付的订单可以取消）
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("订单状态异常，无法取消");
        }

        // 执行取消逻辑
        doCancel(order);

        logger.info("订单取消成功，订单号：{}，用户ID：{}", order.getOrderNo(), userId);
    }

    /**
     * 取消过期订单
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrder(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        doCancel(order);
        logger.info("过期订单自动取消，订单号：{}", order.getOrderNo());
    }

    /**
     * 执行取消订单逻辑
     *
     * @param order 订单
     */
    private void doCancel(Order order) {
        // 1. 查询订单座位
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, order.getId());
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        if (!orderSeats.isEmpty()) {
            // 2. 按票档分组，回滚库存
            Map<Long, Long> ticketTypeCountMap = orderSeats.stream()
                    .collect(Collectors.groupingBy(OrderSeat::getTicketTypeId, Collectors.counting()));

            for (Map.Entry<Long, Long> entry : ticketTypeCountMap.entrySet()) {
                LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TicketType::getId, entry.getKey())
                        .setSql("available_stock = available_stock + " + entry.getValue());
                ticketTypeService.update(updateWrapper);
            }

            // 3. 删除订单座位记录
            LambdaQueryWrapper<OrderSeat> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(OrderSeat::getOrderId, order.getId());
            orderSeatService.remove(deleteWrapper);
        }

        // 4. 更新订单状态为已取消
        order.setStatus(OrderStatus.CANCELLED);
        this.updateById(order);
    }

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Override
    @Transactional(readOnly = true)// 开启事务，确保数据一致性
    public OrderResponse getOrderDetail(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在，订单ID：" + orderId);
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setShowId(order.getShowId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setPayTime(order.getPayTime());
        response.setExpireTime(order.getExpireTime());
        response.setPickupCode(order.getPickupCode());
        response.setCreateTime(order.getCreateTime());

        // 获取场次信息
        Show show = showService.getById(order.getShowId());
        if (show != null) {
            response.setShowTime(show.getShowTime());

            // 获取演唱会名称
            Concert concert = concertService.getById(show.getConcertId());
            if (concert != null) {
                response.setConcertName(concert.getName());
            }else {
                logger.warn("订单{}中存在不存在的演唱会{}", orderId, show.getConcertId());
            }

            // 获取场馆名称
            Venue venue = venueService.getById(show.getVenueId());
            if (venue != null) {
                response.setVenueName(venue.getName());
            } else {
                logger.warn("订单{}中存在不存在的场馆{}", orderId, show.getVenueId());
            }
        }else {
            logger.warn("订单{}中存在不存在的场次{}", orderId, show.getId());
        }

        // 获取座位详情
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, orderId);
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        if (!orderSeats.isEmpty()) {
            // 获取座位信息
            List<Long> seatIds = orderSeats.stream()
                    .map(OrderSeat::getSeatId)
                    .collect(Collectors.toList());
            Map<Long, Seat> seatMap = seatService.listByIds(seatIds).stream()
                    .collect(Collectors.toMap(Seat::getId, s -> s));

            // 获取区域信息
            List<Long> areaIds = seatMap.values().stream()
                    .map(Seat::getAreaId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, SeatArea> areaMap = seatAreaService.listByIds(areaIds).stream()
                    .collect(Collectors.toMap(SeatArea::getId, a -> a));

            // 获取票档信息
            List<Long> ticketTypeIds = orderSeats.stream()
                    .map(OrderSeat::getTicketTypeId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, TicketType> ticketTypeMap = ticketTypeService.listByIds(ticketTypeIds).stream()
                    .collect(Collectors.toMap(TicketType::getId, t -> t));

            List<OrderResponse.SeatDetail> seatDetails = orderSeats.stream()
                    .map(os -> {
                        OrderResponse.SeatDetail detail = new OrderResponse.SeatDetail();
                        detail.setSeatId(os.getSeatId());
                        detail.setPrice(os.getPrice());

                        Seat seat = seatMap.get(os.getSeatId());
                        if (seat != null) {
                            detail.setSeatNo(seat.getSeatNo());

                            SeatArea area = areaMap.get(seat.getAreaId());
                            if (area != null) {
                                detail.setAreaName(area.getName());
                            }else {
                                logger.warn("订单{}中存在不存在的区域{}", orderId, seat.getAreaId());
                            }
                        }else {
                            logger.warn("订单{}中存在不存在的座位{}", orderId, os.getSeatId());
                        }

                        TicketType ticketType = ticketTypeMap.get(os.getTicketTypeId());
                        if (ticketType != null) {
                            detail.setTicketTypeName(ticketType.getName());
                        }else{
                            logger.warn("订单{}中存在不存在的票档{}", orderId, os.getTicketTypeId());
                        }

                        return detail;
                    })
                    .collect(Collectors.toList());

            response.setSeats(seatDetails);
        } else {
            response.setSeats(Collections.emptyList());
        }

        return response;
    }
    /**
     * 获取我的订单
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @param page 页码
     * @param size 每页数量
     * @return 订单列表
     */
    @Override
    public PageResponse<OrderResponse> getMyOrders(Long userId, Integer status, Integer page, Integer size) {
        // 1. 构建分页查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        queryWrapper.orderByDesc(Order::getCreateTime);

        // 2. 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order> orderPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        this.page(orderPage, queryWrapper);

        List<Order> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            return new PageResponse<>(orderPage.getCurrent(), orderPage.getSize(),
                    orderPage.getTotal(), Collections.emptyList());
        }

        // 3. 批量查询关联的场次、演唱会、场馆信息
        List<Long> showIds = orders.stream()
                .map(Order::getShowId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Show> showMap = showService.listByIds(showIds).stream()
                .collect(Collectors.toMap(Show::getId, s -> s));

        // 获取场次对应的演唱会和场馆
        List<Long> concertIds = showMap.values().stream()
                .map(Show::getConcertId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Concert> concertMap = concertIds.isEmpty() ? Collections.emptyMap() :
                concertService.listByIds(concertIds).stream()
                        .collect(Collectors.toMap(Concert::getId, c -> c));

        List<Long> venueIds = showMap.values().stream()
                .map(Show::getVenueId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Venue> venueMap = venueIds.isEmpty() ? Collections.emptyMap() :
                venueService.listByIds(venueIds).stream()
                        .collect(Collectors.toMap(Venue::getId, v -> v));

        // 4. 组装响应（列表页不含座位详情，保持轻量）
        List<OrderResponse> responseList = orders.stream()
                .map(order -> {
                    OrderResponse resp = new OrderResponse();
                    resp.setId(order.getId());
                    resp.setOrderNo(order.getOrderNo());
                    resp.setUserId(order.getUserId());
                    resp.setShowId(order.getShowId());
                    resp.setTotalAmount(order.getTotalAmount());
                    resp.setStatus(order.getStatus());
                    resp.setPayTime(order.getPayTime());
                    resp.setExpireTime(order.getExpireTime());
                    resp.setPickupCode(order.getPickupCode());
                    resp.setCreateTime(order.getCreateTime());

                    Show show = showMap.get(order.getShowId());
                    if (show != null) {
                        resp.setShowTime(show.getShowTime());

                        Concert concert = concertMap.get(show.getConcertId());
                        if (concert != null) {
                            resp.setConcertName(concert.getName());
                        }

                        Venue venue = venueMap.get(show.getVenueId());
                        if (venue != null) {
                            resp.setVenueName(venue.getName());
                        }
                    }

                    return resp;
                })
                .collect(Collectors.toList());

        return new PageResponse<>(orderPage.getCurrent(), orderPage.getSize(),
                orderPage.getTotal(), responseList);
    }
    /**
     * 订单退款
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse refundOrder(Long userId, Long orderId) {
        // 1. 查询订单
        Order order = this.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        // 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作此订单");
        }

        // 2. 校验订单状态（只有已支付的订单可以退款）
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("订单状态异常，无法退款");
        }

        // 3. 校验退款时限：演出前 refundBeforeHours 小时可退
        Show show = showService.getById(order.getShowId());
        if (show == null) {
            throw new NotFoundException("关联场次不存在");
        }

        LocalDateTime refundDeadline = show.getShowTime().minusHours(refundBeforeHours);
        if (LocalDateTime.now().isAfter(refundDeadline)) {
            throw new BusinessException("已超过退款截止时间（演出前" + refundBeforeHours + "小时），无法退款");
        }

        // 4. 回滚库存 + 释放座位
        doRefund(order);

        logger.info("订单退款成功，订单号：{}，用户ID：{}，退款金额：{}", order.getOrderNo(), userId, order.getTotalAmount());

        // 5. 发送短信通知（模拟）
        logger.info("【演唱会订票系统】短信通知：您的订单 {} 已退款成功，退款金额：{}元，预计1-3个工作日到账。",
                order.getOrderNo(), order.getTotalAmount());

        return getOrderDetail(orderId);
    }


    /**
     * 完成已结束场次的订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeFinishedOrders() {
        // 1. 查询所有已结束场次（showTime < now 且状态未标记为已结束的）
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.lt(Show::getShowTime, LocalDateTime.now())
                .ne(Show::getStatus, ShowStatus.CANCELLED); // 排除已取消的场次
        List<Show> finishedShows = showService.list(showQuery);

        if (finishedShows.isEmpty()) {
            return;
        }

        List<Long> finishedShowIds = finishedShows.stream()
                .map(Show::getId)
                .collect(Collectors.toList());

        // 2. 查询这些场次中状态为"已支付"的订单
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.in(Order::getShowId, finishedShowIds)
                .eq(Order::getStatus, OrderStatus.PAID); // 已支付
        List<Order> ordersToComplete = this.list(orderQuery);

        if (ordersToComplete.isEmpty()) {
            return;
        }

        logger.info("发现 {} 个已结束场次的已支付订单，开始标记为已完成...", ordersToComplete.size());

        int successCount = 0;
        for (Order order : ordersToComplete) {
            try {
                order.setStatus(OrderStatus.COMPLETED); // 已完成
                this.updateById(order);
                successCount++;
            } catch (Exception e) {
                logger.error("标记订单为已完成失败，订单ID：{}，错误：{}", order.getId(), e.getMessage());
            }
        }

        // 3. 更新已结束场次的状态为"已结束"
        for (Show show : finishedShows) {
            if (show.getStatus() != ShowStatus.ENDED) {
                show.setStatus(ShowStatus.ENDED);
                showService.updateById(show);
            }
        }

        logger.info("订单自动完成处理完毕，成功：{}/{}", successCount, ordersToComplete.size());
    }

    // ==================== 批量取消过期订单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCancelExpiredOrders(List<Long> orderIds) {
        if (CollectionUtil.isEmpty(orderIds)) {
            return 0;
        }

        // 1. 查询这些订单，确保状态为待支付且未过期（二次校验）
        List<Order> orders = list(new LambdaQueryWrapper<Order>()
                .in(Order::getId, orderIds)
                .eq(Order::getStatus, OrderStatus.PENDING)
                .lt(Order::getExpireTime, LocalDateTime.now()));

        if (orders.isEmpty()) {
            return 0;
        }

        // 2. 收集订单ID
        List<Long> validOrderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        // 3. 删除 order_seat 表中这些订单的座位锁定记录
        int deletedSeats = orderSeatMapper.delete(new LambdaQueryWrapper<OrderSeat>()
                .in(OrderSeat::getOrderId, validOrderIds));
        logger.debug("批量取消订单：删除 order_seat 记录 {} 条", deletedSeats);

        // 4. 回滚 ticket_type 的 sold_quantity
        Map<Long, Integer> rollbackMap = new HashMap<>();
        for (Order order : orders) {
            int seatCount = order.getSeatInfo().split(",").length;
            rollbackMap.merge(order.getTicketTypeId(), seatCount, Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : rollbackMap.entrySet()) {
            ticketTypeMapper.updateSoldQuantityDecrement(entry.getKey(), entry.getValue());
        }

        // 5. 更新订单状态为已取消，并获取实际更新的行数
        int updated = baseMapper.update(null, new LambdaUpdateWrapper<Order>()
                .in(Order::getId, validOrderIds)
                .set(Order::getStatus, OrderStatus.CANCELLED)
                .set(Order::getUpdateTime, LocalDateTime.now()));

        logger.info("批量取消订单完成，成功取消 {} 个订单，回滚座位 {} 个", updated, deletedSeats);
        return updated;
    }

    // ==================== 批量自动完成订单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCompleteFinishedOrders(int batchSize) {
        // 查询需要完成的订单：已支付，且对应的演出场次已经结束（show_date + show_time < now）
        // 注意：这里需要关联 show 表，但为了简化，可以在 Order 表中冗余一个字段 show_end_time，
        // 或者在 SQL 中直接关联。由于我们没有冗余字段，这里使用子查询或 join。
        // 为了性能，我们直接在 Mapper 中编写 SQL。这里演示使用 MyBatis-Plus 的灵活方式。

        // 方法1：使用自定义 Mapper 方法
        // return baseMapper.completeFinishedOrdersBatch(batchSize);

        // 方法2：使用简单的分步查询 + 更新（适合数据量不大的情况）
        // 查询前 batchSize 条需要完成的订单ID
        List<Long> orderIds = baseMapper.selectNeedCompleteOrderIds(batchSize);
        if (orderIds.isEmpty()) {
            return 0;
        }

        // 更新订单状态为已完成
        int updated = baseMapper.update(null,new LambdaUpdateWrapper<Order>()
                .in(Order::getId, orderIds)
                .set(Order::getStatus, OrderStatus.COMPLETED)
                .set(Order::getUpdateTime, LocalDateTime.now()));
        logger.info("批量自动完成订单 {} 个", updated);
        return updated;
    }


    /**
     * 根据手机号查询已支付且有取票码的订单
     *
     * @param phone 手机号
     * @return 订单列表
     */
    @Override
    public List<Order> getPaidOrdersWithTicketCodeByPhone(String phone) {
        // 关联用户表查询
        return baseMapper.selectPaidOrdersWithTicketCodeByPhone(phone);
    }

    /**
     * 核销取票码
     *
     * @param ticketCode 取票码
     * @return 是否核销成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyTicketCode(String ticketCode) {
        int updated = baseMapper.verifyTicketCode(ticketCode);
        return updated > 0;
    }


    /**
     * 执行退款逻辑：回滚库存 + 释放座位 + 更新订单状态
     *
     * @param order 订单
     */
    private void doRefund(Order order) {
        // 1. 查询订单座位
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, order.getId());
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        if (!orderSeats.isEmpty()) {
            // 2. 按票档分组，回滚库存
            Map<Long, Long> ticketTypeCountMap = orderSeats.stream()
                    .collect(Collectors.groupingBy(OrderSeat::getTicketTypeId, Collectors.counting()));

            for (Map.Entry<Long, Long> entry : ticketTypeCountMap.entrySet()) {
                LambdaUpdateWrapper<TicketType> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(TicketType::getId, entry.getKey())
                        .setSql("available_stock = available_stock + " + entry.getValue());
                ticketTypeService.update(updateWrapper);
                logger.info("退款释放座位，订单号：{}，票档数量明细：{}", order.getOrderNo(), ticketTypeCountMap);
            }

            // 3. 删除订单座位记录（释放座位）
            LambdaQueryWrapper<OrderSeat> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(OrderSeat::getOrderId, order.getId());
            orderSeatService.remove(deleteWrapper);
        }

        // 4. 更新订单状态为已退款
        order.setStatus(OrderStatus.REFUNDED);
        this.updateById(order);
    }

    /**
     * 生成订单号
     *
     * @return 订单号
     */
    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 20);
    }

    //短信验证码和取票码的生成使用了 java.util.Random，这是可预测的伪随机数生成器。需要替换为 java.security.SecureRandom
    // 防止验证码被猜测。

//    /**
//     * 生成取票码（6位字母数字）
//     */
//    private String generatePickupCode() {
//        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//        Random random = new Random();
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < 6; i++) {
//            sb.append(chars.charAt(random.nextInt(chars.length())));
//        }
//        return sb.toString();
//    }
}
