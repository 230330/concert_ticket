package com.concert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concert.dto.response.ConcertDetailResponse;
import com.concert.dto.response.ConcertListResponse;
import com.concert.dto.response.PageResponse;
import com.concert.entity.*;
import com.concert.enums.ConcertStatus;
import com.concert.enums.ShowStatus;
import com.concert.exception.NotFoundException;
import com.concert.mapper.ConcertMapper;
import com.concert.service.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:    演出信息服务实现类
 * @author: hzf
 * @date: 2026-04-17 15:30
 */

@Service
public class ConcertServiceImpl extends ServiceImpl<ConcertMapper, Concert> implements ConcertService {

    @Resource
    private ConcertArtistService concertArtistService;

    @Resource
    private ArtistService artistService;

    @Resource
    private ShowService showService;

    @Resource
    private VenueService venueService;

    @Resource
    private TicketTypeService ticketTypeService;

    /**
     * 分页查询热门演唱会列表
     * <p>
     * 热门演唱会的定义：状态为“未开始”或“进行中”的演唱会。
     * 排序规则：
     * <ul>
     *     <li>当 {@code sort} 参数为 {@code "time"} 时，按演唱会创建时间倒序排列（最新创建的优先）</li>
     *     <li>否则按主键 ID 倒序排列（通常 ID 越大表示越新录入）</li>
     * </ul>
     * </p>
     *
     * @param page 当前页码（从 1 开始），已由 Controller 层校验并规范化
     * @param size 每页条数，已由 Controller 层校验并限制最大值
     * @param sort 排序方式，支持 {@code "time"} 表示按时间排序，其他值按默认排序（按 ID）
     * @return 包含当前页演唱会列表的分页响应对象（PageResponse&lt;ConcertListResponse&gt;）
     */
    @Override
    public PageResponse<ConcertListResponse> getHotConcerts(Integer page, Integer size, String sort) {
        // ========== 1. 创建 MyBatis-Plus 分页对象 ==========
        // 该对象会封装分页参数（页码、每页条数），并在执行查询后自动填充总记录数、当前页数据等
        Page<Concert> concertPage = new Page<>(page, size);

        // ========== 2. 构建查询条件 ==========
        // 使用 Lambda 查询包装器，类型安全，避免硬编码字段名
        LambdaQueryWrapper<Concert> queryWrapper = new LambdaQueryWrapper<>();

        // 2.1 状态筛选：只查询状态为“未开始”或“进行中”的演唱会（热门演出不包含已结束的）
        queryWrapper.in(Concert::getStatus, ConcertStatus.NOT_STARTED, ConcertStatus.IN_PROGRESS);

        // 2.2 动态排序：根据前端传入的 sort 参数决定排序字段
        if ("time".equals(sort)) {
            // 按创建时间倒序（最新发布的优先展示）
            queryWrapper.orderByDesc(Concert::getCreateTime);
        } else {
            // 默认按主键 ID 倒序（通常 ID 递增代表录入时间递增，效果接近按时间排序）
            queryWrapper.orderByDesc(Concert::getId);
        }

        // ========== 3. 执行分页查询 ==========
        // this.page() 是 MyBatis-Plus 提供的分页查询方法（当前类继承自 ServiceImpl）
        // 执行后，concertPage 对象中会填充：
        //   - records: 当前页的数据列表（List<Concert>）
        //   - total:   符合条件的总记录数
        //   - current: 当前页码（即传入的 page）
        //   - size:    每页条数（即传入的 size）
        this.page(concertPage, queryWrapper);

        // ========== 4. 实体 → 响应 DTO 转换 ==========
        // 将当前页的 Concert 实体列表转换为前端需要的 ConcertListResponse 列表
        // （只提取列表页需要的字段，如名称、封面、时间等，不包含详情字段）
        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());

        // ========== 5. 封装并返回分页响应 ==========
        // PageResponse 是统一的分页响应格式，包含页码、每页条数、总记录数、数据列表
        return new PageResponse<>(
                // 当前页码
                concertPage.getCurrent(),
                // 每页条数
                concertPage.getSize(),
                // 总记录数
                concertPage.getTotal(),
                // 当前页数据（已转换）
                responseList
        );
    }

    /**
     * 分页查询即将开始的演唱会列表
     * <p>
     * 即将开始的演唱会定义为：该演唱会下至少存在一个满足以下条件的场次：
     * <ul>
     *     <li>演出时间在当前时间之后（未开始）</li>
     *     <li>场次状态为“未开售”或“正在销售”</li>
     * </ul>
     * 查询逻辑：
     * <ol>
     *     <li>先查询所有符合条件的场次（按演出时间升序）</li>
     *     <li>提取这些场次所属的演唱会 ID（去重）</li>
     *     <li>如果没有符合条件的演唱会，直接返回空分页结果</li>
     *     <li>否则根据演唱会 ID 集合分页查询演唱会基本信息（状态过滤）</li>
     * </ol>
     * 最终返回的演唱会列表顺序与分页参数有关（由主键 ID 倒序，或可按需调整），
     * 而非按场次时间排序，因为一个演唱会可能对应多个场次，分页针对的是演唱会本身。
     * </p>
     *
     * @param page 当前页码（从 1 开始），已由 Controller 层校验并规范化
     * @param size 每页条数，已由 Controller 层校验并限制最大值
     * @return 包含即将开始的演唱会列表的分页响应对象（PageResponse&lt;ConcertListResponse&gt;）
     */
    @Override
    public PageResponse<ConcertListResponse> getUpcomingConcerts(Integer page, Integer size) {
        // 获取当前时间，用于判断场次是否即将开始（演出时间 > 当前时间）
        LocalDateTime now = LocalDateTime.now();

        // ========== 1. 查询所有“即将开始”的场次 ==========
        // 构建场次查询条件
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.gt(Show::getShowTime, now)                         // 演出时间 > 当前时间
                .in(Show::getStatus, ShowStatus.NOT_ON_SALE, ShowStatus.ON_SALE) // 状态：未开售或正在销售
                .orderByAsc(Show::getShowTime);                    // 按演出时间升序（最早的即将开始的场次在前）

        // 执行查询，获取所有符合条件的场次列表
        // 注意：这里没有分页，因为需要先收集所有关联的演唱会 ID，但场次数量通常不会太多（可接受全量查询）
        List<Show> upcomingShows = showService.list(showQuery);

        // ========== 2. 提取场次所属的演唱会 ID（去重） ==========
        // 使用 Stream API 提取 ConcertId，并去重（同一个演唱会可能有多个场次）
        List<Long> concertIds = upcomingShows.stream()
                .map(Show::getConcertId)      // 获取每个场次的演唱会 ID
                .distinct()                   // 去重
                .collect(Collectors.toList());

        // ========== 3. 如果没有找到任何符合条件的场次，直接返回空分页结果 ==========
        // 避免后续无效的数据库查询
        if (concertIds.isEmpty()) {
            // 返回一个空的分页响应：第 1 页，每页 size 条，总记录数为 0，数据为空列表
            return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
        }

        // ========== 4. 根据演唱会 ID 集合分页查询演唱会基本信息 ==========
        // 4.1 创建 MyBatis-Plus 分页对象
        Page<Concert> concertPage = new Page<>(page, size);

        // 4.2 构建演唱会查询条件：ID 在 concertIds 集合中，且状态为“未开始”或“进行中”
        LambdaQueryWrapper<Concert> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Concert::getId, concertIds)                           // 只查询有即将开始场次的演唱会
                .in(Concert::getStatus, ConcertStatus.NOT_STARTED, ConcertStatus.IN_PROGRESS); // 状态有效

        // 4.3 执行分页查询
        // this.page() 会填充 concertPage 的 records（当前页数据）、total（总记录数）等
        this.page(concertPage, queryWrapper);

        // ========== 5. 实体 → 响应 DTO 转换 ==========
        // 将当前页的 Concert 实体列表转换为前端需要的 ConcertListResponse 列表
        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());

        // ========== 6. 封装并返回分页响应 ==========
        return new PageResponse<>(
                concertPage.getCurrent(),   // 当前页码
                concertPage.getSize(),      // 每页条数
                concertPage.getTotal(),     // 总记录数（符合条件的演唱会总数，不是场次数）
                responseList                // 当前页演唱会数据
        );
    }

    /**
     * 搜索演唱会
     *
     * @param keyword    关键词
     * @param city       城市
     * @param artistName 艺人名称
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param page       当前页码
     * @param size       每页大小
     * @return 搜索结果分页响应
     */
    @Override
    public PageResponse<ConcertListResponse> searchConcerts(String keyword, String city, String artistName,
                                                             LocalDate startDate, LocalDate endDate,
                                                             Integer page, Integer size) {
        // 1. 基础条件：状态为“未开始”或“进行中”
        LambdaQueryWrapper<Concert> concertQuery = new LambdaQueryWrapper<>();
        concertQuery.in(Concert::getStatus, ConcertStatus.NOT_STARTED, ConcertStatus.IN_PROGRESS);

        // 2. 关键词模糊匹配：名称包含关键词
        if (keyword != null && !keyword.trim().isEmpty()) {
            concertQuery.like(Concert::getName, keyword.trim());
        }

        // 3. 艺人名称模糊匹配：艺人名称包含关键词
        if (artistName != null && !artistName.trim().isEmpty()) {
            LambdaQueryWrapper<Artist> artistQuery = new LambdaQueryWrapper<>();
            artistQuery.like(Artist::getName, artistName.trim());
            List<Artist> matchedArtists = artistService.list(artistQuery);

            if (matchedArtists.isEmpty()) {
                return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
            }

            List<Long> artistIds = matchedArtists.stream()
                    .map(Artist::getId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<ConcertArtist> caQuery = new LambdaQueryWrapper<>();
            caQuery.in(ConcertArtist::getArtistId, artistIds);
            List<ConcertArtist> concertArtists = concertArtistService.list(caQuery);

            List<Long> concertIdsByArtist = concertArtists.stream()
                    .map(ConcertArtist::getConcertId)
                    .distinct()
                    .collect(Collectors.toList());

            if (concertIdsByArtist.isEmpty()) {
                return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
            }

            concertQuery.in(Concert::getId, concertIdsByArtist);
        }

        // 4. 按城市或日期范围筛选：按城市筛选，或按日期范围筛选，或按城市和日期范围筛选
        boolean needShowFilter = (city != null && !city.trim().isEmpty())
                || startDate != null || endDate != null;

        if (needShowFilter) {
            LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
            showQuery.in(Show::getStatus, ShowStatus.NOT_ON_SALE, ShowStatus.ON_SALE);

            if (startDate != null) {
                showQuery.ge(Show::getShowTime, startDate.atStartOfDay());
            }
            if (endDate != null) {
                showQuery.le(Show::getShowTime, endDate.atTime(LocalTime.MAX));
            }

            List<Show> shows = showService.list(showQuery);

            if (shows.isEmpty()) {
                return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
            }

            if (city != null && !city.trim().isEmpty()) {
                List<Long> venueIds = shows.stream()
                        .map(Show::getVenueId)
                        .distinct()
                        .collect(Collectors.toList());

                LambdaQueryWrapper<Venue> venueQuery = new LambdaQueryWrapper<>();
                venueQuery.in(Venue::getId, venueIds)
                        .eq(Venue::getCity, city.trim());
                List<Venue> matchedVenues = venueService.list(venueQuery);

                if (matchedVenues.isEmpty()) {
                    return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
                }

                Set<Long> matchedVenueIds = matchedVenues.stream()
                        .map(Venue::getId)
                        .collect(Collectors.toSet());

                shows = shows.stream()
                        .filter(show -> matchedVenueIds.contains(show.getVenueId()))
                        .collect(Collectors.toList());

                if (shows.isEmpty()) {
                    return new PageResponse<>(1L, (long) size, 0L, Collections.emptyList());
                }
            }

            List<Long> concertIdsByShow = shows.stream()
                    .map(Show::getConcertId)
                    .distinct()
                    .collect(Collectors.toList());

            concertQuery.in(Concert::getId, concertIdsByShow);
        }

        // 5. 分页查询：按 ID 倒序排列，确保结果顺序一致
        concertQuery.orderByDesc(Concert::getId);
        Page<Concert> concertPage = new Page<>(page, size);
        this.page(concertPage, concertQuery);

        List<ConcertListResponse> responseList = convertToConcertListResponse(concertPage.getRecords());
        return new PageResponse<>(concertPage.getCurrent(), concertPage.getSize(),
                concertPage.getTotal(), responseList);
    }

    /**
     * 根据演唱会 ID 获取完整的演唱会详情信息
     * <p>
     * 该方法会依次查询：
     * <ul>
     *     <li>演唱会基本信息（Concert 表）</li>
     *     <li>关联的艺人列表（通过中间表 ConcertArtist + Artist 表）</li>
     *     <li>关联的场次列表（Show 表），并附带每个场次的场馆信息（Venue 表）</li>
     * </ul>
     * </p>
     *
     * @param id 演唱会主键 ID，不可为 null
     * @return 包含演唱会完整信息的 ConcertDetailResponse 对象
     * @throws NotFoundException 如果指定 ID 的演唱会不存在
     */
    @Override
    public ConcertDetailResponse getConcertDetail(Long id) {
        // ========== 1. 查询并校验演唱会基本信息 ==========
        // 根据主键直接查询演唱会实体（MyBatis-Plus 提供的方法）
        Concert concert = this.getById(id);
        // 如果不存在，立即抛出业务异常（由全局异常处理器转为 404 响应）
        if (concert == null) {
            throw new NotFoundException("演唱会不存在");
        }

        // 创建响应 DTO 对象，准备组装数据
        ConcertDetailResponse response = new ConcertDetailResponse();
        // 复制演唱会基本信息字段（只复制前端需要的，避免暴露多余字段）
        response.setId(concert.getId());
        response.setName(concert.getName());
        response.setPoster(concert.getPoster());
        response.setDescription(concert.getDescription());
        response.setStatus(concert.getStatus());
        response.setCreateTime(concert.getCreateTime());

        // ========== 2. 查询关联的艺人信息 ==========
        // 2.1 构建中间表查询条件：查询该演唱会关联的所有艺人关系记录
        LambdaQueryWrapper<ConcertArtist> caQuery = new LambdaQueryWrapper<>();
        caQuery.eq(ConcertArtist::getConcertId, id);
        List<ConcertArtist> concertArtists = concertArtistService.list(caQuery);

        // 2.2 如果有关联的艺人关系记录
        if (!concertArtists.isEmpty()) {
            // 从中间表记录中提取所有艺人 ID
            // Stream 流水线：流化 → 映射为 artistId → 收集为新 List
            List<Long> artistIds = concertArtists.stream()
                    .map(ConcertArtist::getArtistId)       // 方法引用，等价于 ca -> ca.getArtistId()
                    .collect(Collectors.toList());         // 收集成 List<Long>

            // 2.3 根据艺人 ID 集合，批量查询艺人详细信息（一次 SQL IN 查询，避免 N+1）
            List<Artist> artists = artistService.listByIds(artistIds);

            // 2.4 将 Artist 实体列表转换为响应 DTO 所需的 ArtistInfo 列表
            // 注意：这里只选择前端需要的字段，不暴露冗余信息
            List<ConcertDetailResponse.ArtistInfo> artistInfos = artists.stream()
                    .map(artist -> {
                        ConcertDetailResponse.ArtistInfo info = new ConcertDetailResponse.ArtistInfo();
                        info.setId(artist.getId());
                        info.setName(artist.getName());
                        info.setAvatar(artist.getAvatar());
                        info.setDescription(artist.getDescription());
                        return info;
                    })
                    .collect(Collectors.toList());
            response.setArtists(artistInfos);
        } else {
            // 没有关联艺人时，设置为空列表（避免前端判空）
            response.setArtists(Collections.emptyList());
        }

        // ========== 3. 查询关联的场次信息（含场馆） ==========
        // 3.1 构建场次查询条件：按演唱会 ID 筛选，并按演出时间升序排列
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.eq(Show::getConcertId, id)
                .orderByAsc(Show::getShowTime);
        List<Show> shows = showService.list(showQuery);

        // 3.2 如果存在场次
        if (!shows.isEmpty()) {
            // 提取所有场次涉及的不重复的场馆 ID
            List<Long> venueIds = shows.stream()
                    .map(Show::getVenueId)           // 映射出场馆 ID
                    .distinct()                      // 去重（同一场馆可能被多个场次使用）
                    .collect(Collectors.toList());

            // 3.3 批量查询场馆信息，并转换为 Map<venueId, Venue>，便于后续 O(1) 查找
            Map<Long, Venue> venueMap = venueService.listByIds(venueIds).stream()
                    .collect(Collectors.toMap(Venue::getId, v -> v));

            // 3.4 将 Show 实体列表转换为响应 DTO 所需的 ShowInfo 列表，同时填充场馆信息
            List<ConcertDetailResponse.ShowInfo> showInfos = shows.stream()
                    .map(show -> {
                        ConcertDetailResponse.ShowInfo info = new ConcertDetailResponse.ShowInfo();
                        // 填充场次自身信息
                        info.setId(show.getId());
                        info.setShowTime(show.getShowTime());
                        info.setSaleStartTime(show.getSaleStartTime());
                        info.setSaleEndTime(show.getSaleEndTime());
                        info.setStatus(show.getStatus());

                        // 从 venueMap 中获取场馆详情（避免循环查询数据库）
                        Venue venue = venueMap.get(show.getVenueId());
                        if (venue != null) {
                            info.setVenueName(venue.getName());
                            info.setCity(venue.getCity());
                            info.setAddress(venue.getAddress());
                        }
                        return info;
                    })
                    .collect(Collectors.toList());
            response.setShows(showInfos);
        } else {
            // 没有场次时，设置为空列表
            response.setShows(Collections.emptyList());
        }

        // 返回完整组装好的响应对象
        return response;
    }

    /**
     * 将演唱会实体列表转换为演唱会列表响应对象列表
     *
     * @param concerts 演唱会实体列表
     * @return 演唱会列表响应对象列表
     */
    private List<ConcertListResponse> convertToConcertListResponse(List<Concert> concerts) {
        if (concerts.isEmpty()) {
            return Collections.emptyList();
        }
        //1. 提取所有演唱会 ID
        List<Long> concertIds = concerts.stream()
                .map(Concert::getId)
                .collect(Collectors.toList());

        //2. 批量查询艺人关联
        // 批量查询艺人关联
        LambdaQueryWrapper<ConcertArtist> caQuery = new LambdaQueryWrapper<>();
        caQuery.in(ConcertArtist::getConcertId, concertIds);
        List<ConcertArtist> concertArtists = concertArtistService.list(caQuery);

        //3. 按演唱会分组艺人
        List<Long> artistIds = concertArtists.stream()
                .map(ConcertArtist::getArtistId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> artistNameMap = new HashMap<>();
        if (!artistIds.isEmpty()) {
            artistNameMap = artistService.listByIds(artistIds).stream()
                    .collect(Collectors.toMap(Artist::getId, Artist::getName));
        }

        Map<Long, String> finalArtistNameMap = artistNameMap;
        Map<Long, List<String>> concertArtistNamesMap = concertArtists.stream()
                .collect(Collectors.groupingBy(
                        ConcertArtist::getConcertId,
                        Collectors.mapping(ca -> finalArtistNameMap.get(ca.getArtistId()), Collectors.toList())
                ));

        //4. 批量查询场
        LambdaQueryWrapper<Show> showQuery = new LambdaQueryWrapper<>();
        showQuery.in(Show::getConcertId, concertIds)
                .orderByAsc(Show::getShowTime);
        List<Show> shows = showService.list(showQuery);

        //5. 批量查询场馆
        List<Long> venueIds = shows.stream()
                .map(Show::getVenueId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Venue> venueMap = new HashMap<>();
        if (!venueIds.isEmpty()) {
            venueMap = venueService.listByIds(venueIds).stream()
                    .collect(Collectors.toMap(Venue::getId, v -> v));
        }

        //6. 按演唱会分组场次
        Map<Long, List<Show>> concertShowsMap = shows.stream()
                .collect(Collectors.groupingBy(Show::getConcertId));

        //7. 批量查询票档获取最低价
        List<Long> showIds = shows.stream()
                .map(Show::getId)
                .collect(Collectors.toList());
        Map<Long, BigDecimal> showMinPriceMap = new HashMap<>();
        if (!showIds.isEmpty()) {
            LambdaQueryWrapper<TicketType> ttQuery = new LambdaQueryWrapper<>();
            ttQuery.in(TicketType::getShowId, showIds);
            List<TicketType> ticketTypes = ticketTypeService.list(ttQuery);

            showMinPriceMap = ticketTypes.stream()
                    .collect(Collectors.groupingBy(
                            TicketType::getShowId,
                            Collectors.collectingAndThen(
                                    Collectors.minBy(Comparator.comparing(TicketType::getPrice)),
                                    opt -> opt.map(TicketType::getPrice).orElse(null)
                            )
                    ));
        }

        //8. 组装响应对象：将所有信息填充到 ConcertListResponse 对象中
        Map<Long, Venue> finalVenueMap = venueMap;
        Map<Long, BigDecimal> finalShowMinPriceMap = showMinPriceMap;

        return concerts.stream()
                .map(concert -> {
                    ConcertListResponse resp = new ConcertListResponse();
                    resp.setId(concert.getId());
                    resp.setName(concert.getName());
                    resp.setPoster(concert.getPoster());
                    resp.setStatus(concert.getStatus());
                    resp.setCreateTime(concert.getCreateTime());

                    // 艺人名称
                    resp.setArtistNames(concertArtistNamesMap.getOrDefault(concert.getId(), Collections.emptyList()));

                    // 最近场次信息
                    List<Show> concertShows = concertShowsMap.get(concert.getId());
                    if (concertShows != null && !concertShows.isEmpty()) {
                        Show nearestShow = concertShows.get(0);
                        resp.setNearestShowTime(nearestShow.getShowTime());

                        Venue venue = finalVenueMap.get(nearestShow.getVenueId());
                        if (venue != null) {
                            resp.setCity(venue.getCity());
                            resp.setVenueName(venue.getName());
                        }

                        BigDecimal minPrice = finalShowMinPriceMap.get(nearestShow.getId());
                        if (minPrice != null) {
                            resp.setMinPrice("¥" + minPrice.toString());
                        }
                    }

                    return resp;
                })
                .collect(Collectors.toList());
    }
}
