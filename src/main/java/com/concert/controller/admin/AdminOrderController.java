package com.concert.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.common.Result;
import com.concert.dto.response.AdminOrderResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.exception.BusinessException;
import com.concert.exception.NotFoundException;
import com.concert.service.*;
import com.concert.utils.PageUtil;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description:    管理员订单管理
 * @author: hzf
 * @date: 2026-04-17 15:30
 */
@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private OrderSeatService orderSeatService;

    @Resource
    private UserService userService;

    @Resource
    private ShowService showService;

    @Resource
    private ConcertService concertService;

    @Resource
    private VenueService venueService;

    @Resource
    private SeatService seatService;

    @Resource
    private SeatAreaService seatAreaService;

    @Resource
    private TicketTypeService ticketTypeService;

    /**
     * 分页查询订单列表
     *
     * @param page     页码
     * @param size     每页条数
     * @param status   订单状态筛选
     * @param orderNo  订单编号搜索
     * @param userId   用户ID筛选
     */
    @GetMapping("/list")
    public Result<PageResponse<AdminOrderResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId) {

        int[] params = PageUtil.validate(page, size);

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

        Page<Order> orderPage = new Page<>(params[0], params[1]);
        orderService.page(orderPage, queryWrapper);

        List<Order> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            return Result.success(new PageResponse<>(orderPage.getCurrent(), orderPage.getSize(),
                    orderPage.getTotal(), Collections.emptyList()));
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

            // 用户信息
            User user = userMap.get(order.getUserId());
            if (user != null) {
                resp.setUserPhone(user.getPhone());
                resp.setUserNickname(user.getNickname());
            }

            // 场次/演唱会/场馆
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

        PageResponse<AdminOrderResponse> pageResponse = new PageResponse<>(
                orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal(), responseList);
        return Result.success(pageResponse);
    }

    /**
     * 查询订单详情（含用户信息和座位详情）
     *
     * @param id 订单ID
     */
    @GetMapping("/{id}")
    public Result<AdminOrderResponse> detail(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存在");
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

        // 座位详情（复用 OrderResponse.SeatDetail）
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, id);
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);
        if (!orderSeats.isEmpty()) {
            List<Long> seatIds = orderSeats.stream().map(OrderSeat::getSeatId).collect(Collectors.toList());
            Map<Long, Seat> seatMap = seatService.listByIds(seatIds).stream()
                    .collect(Collectors.toMap(Seat::getId, s -> s));

            List<Long> areaIds = seatMap.values().stream().map(Seat::getAreaId).distinct().collect(Collectors.toList());
            Map<Long, SeatArea> areaMap = areaIds.isEmpty() ? Collections.emptyMap() :
                    seatAreaService.listByIds(areaIds).stream().collect(Collectors.toMap(SeatArea::getId, a -> a));

            List<Long> ticketTypeIds = orderSeats.stream().map(OrderSeat::getTicketTypeId).distinct().collect(Collectors.toList());
            Map<Long, TicketType> ticketTypeMap = ticketTypeIds.isEmpty() ? Collections.emptyMap() :
                    ticketTypeService.listByIds(ticketTypeIds).stream().collect(Collectors.toMap(TicketType::getId, t -> t));

            List<com.concert.dto.response.OrderResponse.SeatDetail> seatDetails = orderSeats.stream().map(os -> {
                com.concert.dto.response.OrderResponse.SeatDetail detail = new com.concert.dto.response.OrderResponse.SeatDetail();
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
            }).collect(Collectors.toList());

            resp.setSeats(seatDetails);
        } else {
            resp.setSeats(Collections.emptyList());
        }

        return Result.success(resp);
    }

    /**
     * 管理员退款（不受退款时限限制）
     *
     * @param id 订单ID
     */
    @PutMapping("/{id}/refund")
    public Result<Void> adminRefund(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        // 只有已支付或已完成的订单可以退款
        if (order.getStatus() != 1 && order.getStatus() != 4) {
            throw new BusinessException("订单状态异常，无法退款");
        }

        // 回滚库存 + 释放座位 + 更新状态
        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.eq(OrderSeat::getOrderId, order.getId());
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        if (!orderSeats.isEmpty()) {
            // 回滚库存
            Map<Long, Long> ticketTypeCountMap = orderSeats.stream()
                    .collect(Collectors.groupingBy(OrderSeat::getTicketTypeId, Collectors.counting()));
            for (Map.Entry<Long, Long> entry : ticketTypeCountMap.entrySet()) {
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TicketType> updateWrapper =
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
                updateWrapper.eq(TicketType::getId, entry.getKey())
                        .setSql("available_stock = available_stock + " + entry.getValue());
                ticketTypeService.update(updateWrapper);
            }

            // 删除订单座位
            LambdaQueryWrapper<OrderSeat> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(OrderSeat::getOrderId, order.getId());
            orderSeatService.remove(deleteWrapper);
        }

        // 更新订单状态为已退款
        order.setStatus(3);
        orderService.updateById(order);

        return Result.success();
    }

    /**
     * 管理员取消订单
     *
     * @param id 订单ID
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> adminCancel(@PathVariable Long id) {
        Order order = orderService.getById(id);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }

        if (order.getStatus() != 0) {
            throw new BusinessException("只有待支付的订单可以取消");
        }

        orderService.cancelExpiredOrder(id);
        return Result.success();
    }
}
