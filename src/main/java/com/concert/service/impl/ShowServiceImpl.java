package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.response.SeatMapResponse;
import com.concert.dto.response.ShowListResponse;
import com.concert.entity.*;
import com.concert.exception.NotFoundException;
import com.concert.mapper.ShowMapper;
import com.concert.service.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:    场次服务实现
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class ShowServiceImpl extends ServiceImpl<ShowMapper, Show> implements ShowService {

    @Resource
    private ConcertService concertService;

    @Resource
    private VenueService venueService;

    @Resource
    private TicketTypeService ticketTypeService;

    @Resource
    private SeatAreaService seatAreaService;

    @Resource
    private SeatService seatService;

    @Resource
    private OrderSeatService orderSeatService;

    @Resource
    private OrderService orderService;

    @Override
    public List<ShowListResponse> getShowList(Long concertId, LocalDate date) {
        // 查询演唱会
        Concert concert = concertService.getById(concertId);
        if (concert == null) {
            throw new NotFoundException("演唱会不存在");
        }

        // 查询场次
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.eq(Show::getConcertId, concertId);

        if (date != null) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            showQuery.between(Show::getShowTime, startOfDay, endOfDay);
        }

        showQuery.orderByAsc(Show::getShowTime);
        List<Show> shows = this.list(showQuery);

        if (shows.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取场馆信息
        List<Long> venueIds = shows.stream()
                .map(Show::getVenueId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Venue> venueMap = venueService.listByIds(venueIds).stream()
                .collect(Collectors.toMap(Venue::getId, v -> v));

        // 获取票档信息
        List<Long> showIds = shows.stream()
                .map(Show::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<TicketType> ttQuery = new LambdaQueryWrapper<>();
        ttQuery.in(TicketType::getShowId, showIds);
        List<TicketType> ticketTypes = ticketTypeService.list(ttQuery);

        // 获取区域信息
        List<Long> areaIds = ticketTypes.stream()
                .map(TicketType::getAreaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SeatArea> areaMap = Collections.emptyMap();
        if (!areaIds.isEmpty()) {
            areaMap = seatAreaService.listByIds(areaIds).stream()
                    .collect(Collectors.toMap(SeatArea::getId, a -> a));
        }

        // 按场次分组票档
        Map<Long, List<TicketType>> showTicketTypesMap = ticketTypes.stream()
                .collect(Collectors.groupingBy(TicketType::getShowId));

        Map<Long, SeatArea> finalAreaMap = areaMap;

        // 构建响应
        return shows.stream()
                .map(show -> {
                    ShowListResponse resp = new ShowListResponse();
                    resp.setId(show.getId());
                    resp.setConcertId(show.getConcertId());
                    resp.setConcertName(concert.getName());
                    resp.setVenueId(show.getVenueId());
                    resp.setShowTime(show.getShowTime());
                    resp.setSaleStartTime(show.getSaleStartTime());
                    resp.setSaleEndTime(show.getSaleEndTime());
                    resp.setStatus(show.getStatus());

                    // 场馆信息
                    Venue venue = venueMap.get(show.getVenueId());
                    if (venue != null) {
                        resp.setVenueName(venue.getName());
                        resp.setCity(venue.getCity());
                        resp.setAddress(venue.getAddress());
                    }

                    // 票档信息
                    List<TicketType> showTicketTypes = showTicketTypesMap.getOrDefault(show.getId(), Collections.emptyList());
                    List<ShowListResponse.TicketTypeInfo> ticketTypeInfos = showTicketTypes.stream()
                            .map(tt -> {
                                ShowListResponse.TicketTypeInfo info = new ShowListResponse.TicketTypeInfo();
                                info.setId(tt.getId());
                                info.setName(tt.getName());
                                info.setPrice(tt.getPrice());
                                info.setTotalStock(tt.getTotalStock());
                                info.setAvailableStock(tt.getAvailableStock());
                                info.setAreaId(tt.getAreaId());

                                SeatArea area = finalAreaMap.get(tt.getAreaId());
                                if (area != null) {
                                    info.setAreaName(area.getName());
                                }
                                return info;
                            })
                            .collect(Collectors.toList());
                    resp.setTicketTypes(ticketTypeInfos);

                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public SeatMapResponse getSeatMap(Long showId) {
        // 查询场次
        Show show = this.getById(showId);
        if (show == null) {
            throw new NotFoundException("场次不存在");
        }

        // 查询场馆
        Venue venue = venueService.getById(show.getVenueId());
        if (venue == null) {
            throw new NotFoundException("场馆不存在");
        }

        SeatMapResponse response = new SeatMapResponse();
        response.setShowId(showId);
        response.setVenueId(venue.getId());
        response.setVenueName(venue.getName());

        // 查询场馆的所有区域
        LambdaQueryWrapper<SeatArea> areaQuery = new LambdaQueryWrapper<>();
        areaQuery.eq(SeatArea::getVenueId, venue.getId());
        List<SeatArea> areas = seatAreaService.list(areaQuery);

        if (areas.isEmpty()) {
            response.setAreas(Collections.emptyList());
            return response;
        }

        // 查询该场次的票档信息
        LambdaQueryWrapper<TicketType> ttQuery = new LambdaQueryWrapper<>();
        ttQuery.eq(TicketType::getShowId, showId);
        List<TicketType> ticketTypes = ticketTypeService.list(ttQuery);
        Map<Long, TicketType> areaTicketTypeMap = ticketTypes.stream()
                .collect(Collectors.toMap(TicketType::getAreaId, t -> t, (a, b) -> a));

        // 获取所有区域ID
        List<Long> areaIds = areas.stream()
                .map(SeatArea::getId)
                .collect(Collectors.toList());

        // 查询所有座位
        LambdaQueryWrapper<Seat> seatQuery = new LambdaQueryWrapper<>();
        seatQuery.in(Seat::getAreaId, areaIds)
                .orderByAsc(Seat::getRowNum)
                .orderByAsc(Seat::getColNum);
        List<Seat> allSeats = seatService.list(seatQuery);

        // 按区域分组座位
        Map<Long, List<Seat>> areaSeatMap = allSeats.stream()
                .collect(Collectors.groupingBy(Seat::getAreaId));

        // 查询已售座位
        Set<Long> soldSeatIds = getSoldSeatIds(showId);

        // 构建区域信息
        List<SeatMapResponse.AreaInfo> areaInfos = areas.stream()
                .map(area -> {
                    SeatMapResponse.AreaInfo areaInfo = new SeatMapResponse.AreaInfo();
                    areaInfo.setId(area.getId());
                    areaInfo.setName(area.getName());
                    areaInfo.setCapacity(area.getCapacity());

                    // 票档信息
                    TicketType ticketType = areaTicketTypeMap.get(area.getId());
                    if (ticketType != null) {
                        areaInfo.setPrice(ticketType.getPrice());
                        areaInfo.setTicketTypeId(ticketType.getId());
                        areaInfo.setTicketTypeName(ticketType.getName());
                    }

                    // 座位信息（按行分组）
                    List<Seat> areaSeats = areaSeatMap.getOrDefault(area.getId(), Collections.emptyList());
                    Map<Integer, List<Seat>> rowSeatMap = areaSeats.stream()
                            .collect(Collectors.groupingBy(Seat::getRowNum, TreeMap::new, Collectors.toList()));

                    List<SeatMapResponse.RowInfo> rows = rowSeatMap.entrySet().stream()
                            .map(entry -> {
                                SeatMapResponse.RowInfo rowInfo = new SeatMapResponse.RowInfo();
                                rowInfo.setRowNum(entry.getKey());

                                List<SeatMapResponse.SeatInfo> seatInfos = entry.getValue().stream()
                                        .map(seat -> {
                                            SeatMapResponse.SeatInfo seatInfo = new SeatMapResponse.SeatInfo();
                                            seatInfo.setId(seat.getId());
                                            seatInfo.setRowNum(seat.getRowNum());
                                            seatInfo.setColNum(seat.getColNum());
                                            seatInfo.setSeatNo(seat.getSeatNo());
                                            seatInfo.setSold(soldSeatIds.contains(seat.getId()));
                                            return seatInfo;
                                        })
                                        .collect(Collectors.toList());
                                rowInfo.setSeats(seatInfos);
                                return rowInfo;
                            })
                            .collect(Collectors.toList());

                    areaInfo.setRows(rows);
                    return areaInfo;
                })
                .collect(Collectors.toList());

        response.setAreas(areaInfos);
        return response;
    }

    /**
     * 获取已售座位ID集合
     *
     * @param showId 场次ID
     * @return 已售座位ID集合
     */
    private Set<Long> getSoldSeatIds(Long showId) {
        LambdaQueryWrapper<Order> orderQuery = new LambdaQueryWrapper<>();
        orderQuery.eq(Order::getShowId, showId)
                .in(Order::getStatus, 0, 1, 4);
        List<Order> orders = orderService.list(orderQuery);

        if (orders.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<OrderSeat> osQuery = new LambdaQueryWrapper<>();
        osQuery.in(OrderSeat::getOrderId, orderIds);
        List<OrderSeat> orderSeats = orderSeatService.list(osQuery);

        return orderSeats.stream()
                .map(OrderSeat::getSeatId)
                .collect(Collectors.toSet());
    }
}
