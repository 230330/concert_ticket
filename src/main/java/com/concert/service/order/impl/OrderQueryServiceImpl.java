package com.concert.service.order.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.dto.response.AdminOrderResponse;
import com.concert.dto.response.OrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.enums.OrderStatus;
import com.concert.exception.NotFoundException;
import com.concert.mapper.OrderMapper;
import com.concert.service.*;
import com.concert.service.order.OrderQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 订单查询服务实现
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private static final Logger logger = LoggerFactory.getLogger(OrderQueryServiceImpl.class);

    @Resource
    private OrderService orderService;

    @Resource
    private ShowService showService;

    @Resource
    private ConcertService concertService;

    @Resource
    private VenueService venueService;

    @Resource
    private UserService userService;

    @Resource
    private SeatService seatService;

    @Resource
    private SeatAreaService seatAreaService;

    @Resource
    private TicketTypeService ticketTypeService;

    @Resource
    private OrderSeatService orderSeatService;

    @Resource
    private OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        Order order = orderService.getById(orderId);
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

            Concert concert = concertService.getById(show.getConcertId());
            if (concert != null) {
                response.setConcertName(concert.getName());
            } else {
                logger.warn("订单{}中存在不存在的演唱会{}", orderId, show.getConcertId());
            }

            Venue venue = venueService.getById(show.getVenueId());
            if (venue != null) {
                response.setVenueName(venue.getName());
            } else {
                logger.warn("订单{}中存在不存在的场馆{}", orderId, show.getVenueId());
            }
        } else {
            logger.warn("订单{}中存在不存在的场次{}", orderId, order.getShowId());
        }

        // 获取座位详情
        List<OrderResponse.SeatDetail> seatDetails = buildSeatDetails(orderId);
        response.setSeats(seatDetails);

        return response;
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(Long userId, Integer status, Integer page, Integer size) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getUserId, userId);
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        queryWrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = new Page<>(page, size);
        orderService.page(orderPage, queryWrapper);

        List<Order> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            return new PageResponse<>(orderPage.getCurrent(), orderPage.getSize(),
                    orderPage.getTotal(), Collections.emptyList());
        }

        // 批量查询关联数据
        List<Long> showIds = orders.stream().map(Order::getShowId).distinct().collect(Collectors.toList());
        Map<Long, Show> showMap = showService.listByIds(showIds).stream()
                .collect(Collectors.toMap(Show::getId, s -> s));

        List<Long> concertIds = showMap.values().stream().map(Show::getConcertId).distinct().collect(Collectors.toList());
        Map<Long, Concert> concertMap = concertIds.isEmpty() ? Collections.emptyMap() :
                concertService.listByIds(concertIds).stream().collect(Collectors.toMap(Concert::getId, c -> c));

        List<Long> venueIds = showMap.values().stream().map(Show::getVenueId).distinct().collect(Collectors.toList());
        Map<Long, Venue> venueMap = venueIds.isEmpty() ? Collections.emptyMap() :
                venueService.listByIds(venueIds).stream().collect(Collectors.toMap(Venue::getId, v -> v));

        // 组装响应（列表页不含座位详情，保持轻量）
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

    @Override
    public PageResponse<AdminOrderResponse> listOrdersForAdmin(int page, int size, Integer status, String orderNo, Long userId) {
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            queryWrapper.like(Order::getOrderNo, orderNo.trim());
        }
        if (userId != null) {
            queryWrapper.eq(Order::getUserId, userId);
        }
        queryWrapper.orderByDesc(Order::getId);

        Page<Order> orderPage = new Page<>(page, size);
        orderService.page(orderPage, queryWrapper);

        List<Order> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            return new PageResponse<>(orderPage.getCurrent(), orderPage.getSize(),
                    orderPage.getTotal(), Collections.emptyList());
        }

        // 批量查询关联数据
        List<Long> userIds = orders.stream().map(Order::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> showIds = orders.stream().map(Order::getShowId).distinct().collect(Collectors.toList());
        Map<Long, Show> showMap = showService.listByIds(showIds).stream()
                .collect(Collectors.toMap(Show::getId, s -> s));

        List<Long> concertIds = showMap.values().stream().map(Show::getConcertId).distinct().collect(Collectors.toList());
        Map<Long, Concert> concertMap = concertIds.isEmpty() ? Collections.emptyMap() :
                concertService.listByIds(concertIds).stream().collect(Collectors.toMap(Concert::getId, c -> c));

        List<Long> venueIds = showMap.values().stream().map(Show::getVenueId).distinct().collect(Collectors.toList());
        Map<Long, Venue> venueMap = venueIds.isEmpty() ? Collections.emptyMap() :
                venueService.listByIds(venueIds).stream().collect(Collectors.toMap(Venue::getId, v -> v));

        List<AdminOrderResponse> responseList = orders.stream().map(order -> {
            AdminOrderResponse resp = new AdminOrderResponse();
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

            User user = userMap.get(order.getUserId());
            if (user != null) {
                resp.setUserPhone(user.getPhone());
                resp.setUserNickname(user.getNickname());
            }

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
        }).collect(Collectors.toList());

        return new PageResponse<>(orderPage.getCurrent(), orderPage.getSize(),
                orderPage.getTotal(), responseList);
    }

    @Override
    public AdminOrderResponse getOrderDetailForAdmin(Long orderId) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        AdminOrderResponse resp = new AdminOrderResponse();
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

        // 用户信息
        User user = userService.getById(order.getUserId());
        if (user != null) {
            resp.setUserPhone(user.getPhone());
            resp.setUserNickname(user.getNickname());
        }

        // 场次信息
        Show show = showService.getById(order.getShowId());
        if (show != null) {
            resp.setShowTime(show.getShowTime());
            Concert concert = concertService.getById(show.getConcertId());
            if (concert != null) {
                resp.setConcertName(concert.getName());
            }
            Venue venue = venueService.getById(show.getVenueId());
            if (venue != null) {
                resp.setVenueName(venue.getName());
            }
        }

        // 座位详情
        List<OrderResponse.SeatDetail> seatDetails = buildSeatDetails(orderId);
        resp.setSeats(seatDetails);

        return resp;
    }

    @Override
    public List<Order> getPaidOrdersWithTicketCodeByPhone(String phone) {
        return orderMapper.selectPaidOrdersWithTicketCodeByPhone(phone);
    }

    @Override
    public boolean verifyTicketCode(String ticketCode) {
        int updated = orderMapper.verifyTicketCode(ticketCode);
        return updated > 0;
    }

    /**
     * 构建订单座位详情
     */
    private List<OrderResponse.SeatDetail> buildSeatDetails(Long orderId) {
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, orderId);
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        if (orderSeats.isEmpty()) {
            return Collections.emptyList();
        }

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
        Map<Long, SeatArea> areaMap = areaIds.isEmpty() ? Collections.emptyMap() :
                seatAreaService.listByIds(areaIds).stream()
                        .collect(Collectors.toMap(SeatArea::getId, a -> a));

        // 获取票档信息
        List<Long> ticketTypeIds = orderSeats.stream()
                .map(OrderSeat::getTicketTypeId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, TicketType> ticketTypeMap = ticketTypeIds.isEmpty() ? Collections.emptyMap() :
                ticketTypeService.listByIds(ticketTypeIds).stream()
                        .collect(Collectors.toMap(TicketType::getId, t -> t));

        return orderSeats.stream()
                .map(os -> {
                    OrderResponse.SeatDetail detail = new OrderResponse.SeatDetail();
                    detail.setSeatId(os.getSeatId());
                    detail.setPrice(os.getPrice());

                    Seat seat = seatMap.get(os.getSeatId());
                    if (seat != null) {
                        detail.setSeatNo(seat.getSeatCode());

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
    }
}
