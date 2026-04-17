package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.request.CreateOrderRequest;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.exception.BusinessException;
import com.concert.exception.ForbiddenException;
import com.concert.exception.NotFoundException;
import com.concert.mapper.OrderMapper;
import com.concert.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. 获取场次信息
        Show show = showService.getById(request.getShowId());
        if (show == null) {
            throw new NotFoundException("场次不存在");
        }

        // 检查场次状态（1-售票中）
        if (show.getStatus() != 1) {
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
        order.setStatus(0); // 待支付
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
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态异常，无法支付");
        }

        // 校验是否过期
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException("订单已过期，请重新下单");
        }

        // 3. 更新订单状态
        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setPickupCode(generatePickupCode()); // 生成取票码
        this.updateById(order);

        logger.info("订单支付成功，订单号：{}，取票码：{}", order.getOrderNo(), order.getPickupCode());

        // 4. 发送短信通知（模拟）
        logger.info("【演唱会订票系统】短信通知：您的订单 {} 已支付成功，取票码：{}，请妥善保管。",
                order.getOrderNo(), order.getPickupCode());

        return getOrderDetail(orderId);
    }

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
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态异常，无法取消");
        }

        // 执行取消逻辑
        doCancel(order);

        logger.info("订单取消成功，订单号：{}，用户ID：{}", order.getOrderNo(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrder(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null || order.getStatus() != 0) {
            return;
        }

        doCancel(order);
        logger.info("过期订单自动取消，订单号：{}", order.getOrderNo());
    }

    /**
     * 执行取消订单逻辑
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
        order.setStatus(2);
        this.updateById(order);
    }

    @Override
    public OrderResponse getOrderDetail(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            return null;
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
            }

            // 获取场馆名称
            Venue venue = venueService.getById(show.getVenueId());
            if (venue != null) {
                response.setVenueName(venue.getName());
            }
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
                            }
                        }

                        TicketType ticketType = ticketTypeMap.get(os.getTicketTypeId());
                        if (ticketType != null) {
                            detail.setTicketTypeName(ticketType.getName());
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
     * 生成订单号
     */
    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 20);
    }

    /**
     * 生成取票码（6位字母数字）
     */
    private String generatePickupCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
