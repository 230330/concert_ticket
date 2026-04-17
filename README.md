这是一个基于 Spring Boot 2.7 + MyBatis-Plus + Spring Security + Redis 的演唱会订票系统后端项目，采用经典的分层架构。以下是各层的具体分析：
整体项目结构：
com.concert
├── ConcertTicketApplication.java   # 启动类
├── common/                         # 通用层
├── config/                         # 配置层（含 security 子包）
├── controller/                     # 控制器层（含 admin 子包）
├── dto/                            # 数据传输对象（request / response）
├── entity/                         # 实体层
├── mapper/                         # 持久层
├── service/                        # 服务层（含 impl 子包）
├── task/                           # 定时任务层
└── utils/                          # 工具层
1. common — 通用响应封装
   只有一个 Result<T> 类，是全局统一的 API 响应格式，包含 code、message、data 三个字段。提供了 success()、error()、unauthorized()、forbidden() 等静态工厂方法，被所有 Controller 和安全异常处理器使用，确保接口返回格式一致。

2. config — 配置层
   这一层负责框架级别的基础设施配置，分为三个部分：

MybatisPlusConfig：注册分页插件 PaginationInnerInterceptor（演唱会列表分页查询依赖它）以及自动填充处理器，在数据插入/更新时自动写入 createTime 和 updateTime。

RedisConfig：配置 RedisTemplate<String, Object>，使用 Jackson 序列化 value，支持 Java 8 时间类型。主要被短信验证码服务使用。

security 子包（共 6 个类）：这是整个认证鉴权体系的核心。SecurityConfig 定义了过滤链——关闭 CSRF、启用 CORS、无状态会话、公开接口白名单（登录/注册/短信/演唱会浏览等），其余接口均需认证。

JwtAuthenticationTokenFilter 是自定义过滤器，拦截每个请求，从 Authorization 头提取 JWT、验证有效性、加载用户信息到 SecurityContext。LoginUser 实现了 UserDetails，以手机号作为登录标识。

UserDetailsServiceImpl 通过 UserMapper 按手机号查库构建 LoginUser。AccessDeniedHandlerImpl 和 AuthenticationEntryPointImpl 分别处理 403 和 401 的 JSON 响应。

3. controller — 控制器层
   共 4 个控制器，负责接收 HTTP 请求、参数校验、调用 Service 并组装返回数据：

ConcertController (/api/concert)：提供热门演唱会分页列表、即将开演列表、演唱会详情（含艺人、场次信息）。这个控制器内部有较多数据组装逻辑——批量查询艺人、场次、场馆、票种，避免 N+1 问题。

ShowController (/api/show)：查询某演唱会下的场次列表（含票价、库存、区域信息），以及场次座位图（按区域→排→座组织，标记已售状态）。已售判断是通过查询关联订单中状态为待支付/已支付/已完成的 OrderSeat 记录。

OrderController (/api/order)：需登录访问，提供创建订单、模拟支付、取消订单、查看订单详情四个接口。通过 SecurityContext 提取当前用户 ID。

UserController (/api/user)：登录、注册（含短信验证码校验）、发送验证码、查看/修改个人信息。登录时通过 AuthenticationManager 验证凭据后由 JwtUtil 签发 token。


4. dto — 数据传输对象
   分为 request 和 response 两个子包，起到前后端数据隔离的作用：

Request（7 个）：CreateOrderRequest（showId + ticketTypeId + seatIds）、LoginRequest/RegisterRequest/SendSmsRequest（手机号正则校验 ^1[3-9]\d{9}$）、PayOrderRequest/CancelOrderRequest（orderId）、UserUpdateRequest（
昵称/头像/邮箱，均可选）。均使用 @Valid 注解做参数校验。

Response（8 个）：ConcertListResponse/ConcertDetailResponse（演唱会摘要和详情，详情含嵌套的 ArtistInfo 和 ShowInfo 内部类）、ShowListResponse（含票种信息的场次列表）、SeatMapResponse（多层嵌套：区域→排→座位，含 sold 标志）、
OrderResponse（含 statusDesc 状态描述映射和 SeatDetail 座位明细）、LoginResponse（token + 过期时间）、UserInfoResponse、PageResponse<T>（通用分页包装）。

5. entity — 实体层
   共 11 个实体，对应数据库表，使用 Lombok @Data 和 MyBatis-Plus 注解。核心关系是：

User 一对多 Order，Order 一对多 OrderSeat，OrderSeat 关联 Seat。Concert 多对多 Artist（通过 ConcertArtist 中间表），Concert 一对多 Show，Show 属于 Venue，Venue 一对多 SeatArea 一对多 Seat，Show 一对多 TicketType（每种票种关联一个 SeatArea）。

关键状态字段：Concert.status（0 未开始 / 1 进行中 / 2 已结束 / 3 已取消）、Show.status（0 未开售 / 1 售票中 / 2 售罄 / 3 已结束 / 4 已取消）、Order.status（0 待支付 / 1 已支付 / 2 已取消 / 3 已退款 / 4 已完成）。

6. mapper — 持久层
   共 11 个 Mapper 接口，全部继承 MyBatis-Plus 的 BaseMapper<T>，没有自定义 SQL。所有数据库操作依赖 MyBatis-Plus 提供的通用 CRUD 和条件构造器 QueryWrapper/LambdaQueryWrapper（在 Controller 和 Service 中构建查询条件）。

7. service — 服务层
   接口（12 个）：大多数只继承 IService<T> 而无额外方法定义。有自定义方法的是 OrderService（createOrder、payOrder、cancelOrder、cancelExpiredOrder、getOrderDetail）和 SmsCodeService（sendCode、verifyCode）。

实现（14 个）：大部分是空壳（继承 ServiceImpl<Mapper, Entity> 即可），核心业务逻辑集中在两个类：

OrderServiceImpl — 系统最复杂的类，包含完整的订单生命周期管理。createOrder 使用 @Transactional 事务，依次校验场次状态、票种归属、每单限购 4 张、库存充足、座位合法，然后创建订单（15 分钟过期）、批量插入 OrderSeat（利用数据库
(show_id, seat_id) 唯一索引捕获 DuplicateKeyException 防止并发选座冲突）、通过条件 SQL available_stock >= N 原子扣减库存（乐观并发控制）。payOrder 模拟支付（状态翻转 + 生成取票码）。cancelOrder/cancelExpiredOrder 回滚库存、删除 OrderSeat、更新状态。

SmsCodeServiceImpl — 通过 Redis 存储验证码（key 为 sms:code:{phone}，TTL 5 分钟），生成 6 位随机数字码，目前仅日志输出（阿里云短信 SDK 待接入），验证成功后删除 key 实现单次使用。

8. task — 定时任务层
   OrderCleanupTask：使用 @Scheduled(cron = "0 */1 * * * ?") 每分钟执行一次，查询所有 status=0 且 expireTime < 当前时间 的过期未支付订单，逐个调用 OrderService.cancelExpiredOrder() 自动取消并释放座位和库存。

9. utils — 工具层
   JwtUtil：从 application.yml 读取 JWT 密钥和过期时间（24 小时），基于 HMAC-SHA256 算法签发和验证 token，携带 userId 和 phone 两个 claim。被 JwtAuthenticationTokenFilter（验证）和 UserController（签发）使用。

整个系统用乐观锁 + 数据库唯一索引解决了票务系统最核心的并发问题（超卖和重复选座），用轮询式定时任务处理订单过期，用无状态 JWT 实现前后端分离认证。当前 SMS 发送和支付均为模拟实现，属于可扩展的预留接口

---核心业务流程
一、用户认证流程整个认证体系围绕手机号 + 密码 + 短信验证码构建，采用无状态 JWT 机制。用户注册时，先调用 /api/user/sendSms 请求短信验证码，系统生成 6 位随机码存入 Redis（key 为 sms:code:{手机号}，5 分钟过期）。然后用户携带手机号、密码和验证码调用 /api/user/register，后端先通过 SmsCodeService.verifyCode() 校验验证码（匹配后立即从 Redis 删除，确保一次性使用），再检查手机号是否已注册，最后将密码 BCrypt 加密后存入数据库。登录时调用 /api/user/login，Spring Security 的 AuthenticationManager 负责验证手机号和密码。验证通过后 JwtUtil 签发一个携带 userId 和 phone 的 JWT（有效期 24 小时），返回给前端。之后每次请求，JwtAuthenticationTokenFilter 从请求头提取 token、校验有效性、加载用户信息到 SecurityContext，完成无状态认证。

二、演唱会浏览流程这条线路不需要登录（在 SecurityConfig 中配置为 permitAll），是面向所有访客的公开接口。用户可以浏览热门演唱会列表（GET /api/concert/hot，按状态筛选 + 分页）或即将开演的演唱会列表（GET /api/concert/upcoming，关联查询有未来场次的演唱会）。每条演唱会摘要包含名称、海报、参演艺人、最近场次时间、城市、场馆和最低票价。点击某个演唱会可以查看详情（GET /api/concert/{id}），返回完整描述、艺人列表（头像、简介）以及所有场次信息（时间、售票时段、状态、场馆）。选定感兴趣的场次后，调用 GET /api/show/list?concertId=... 查看该演唱会下所有场次的票种信息（票价、总库存、剩余库存、所属区域），再通过 GET /api/show/{showId}/seats 获取座位图。座位图按区域 → 排 → 座的三级结构组织，每个座位标注了 sold 状态——已售判断逻辑是查询该场次下状态为待支付(0)、已支付(1)、已完成(4)的订单关联的 OrderSeat 记录。

三、下单购票流程（核心）这是系统最关键的业务环节，需要登录且在单个数据库事务内完成，集中体现在 OrderServiceImpl.createOrder() 中。用户选好场次、票种和座位后，提交 POST /api/order/create，后端按以下步骤处理：第一步，业务校验：验证场次存在且状态为"售票中"(status=1)；验证票种存在且确实属于该场次；验证选座数量不超过 4 张（单笔限购）；验证剩余库存 availableStock 大于等于选座数量；验证所有座位 ID 真实存在且属于该票种对应的区域。第二步，创建订单：生成 UUID 格式的 20 位订单号，计算总金额（单价 × 座位数），设置订单状态为待支付(0)，过期时间为当前时间 + 15 分钟。第三步，锁定座位：批量插入 OrderSeat 记录。这里利用了数据库在 order_seat 表上 (show_id, seat_id) 的唯一索引——如果两个用户同时选了同一个座位，后提交的那个会触发 DuplicateKeyException，系统捕获后返回"座位已被占用"的错误提示。这是解决并发选座冲突的核心手段。

第四步，扣减库存：通过条件 SQL UPDATE ticket_type SET available_stock = available_stock - N WHERE id = ? AND available_stock >= N 原子操作扣减库存。这是一种乐观并发控制，如果受影响行数为 0 说明库存不足，事务回滚。四、支付流程用户在 15 分钟内调用 POST /api/order/pay，后端校验订单归属当前用户、状态为待支付(0)、未超过过期时间后，将状态改为已支付(1)，记录支付时间，并生成一个 6 位字母数字组合的取票码（pickupCode）。当前支付是模拟实现，没有对接真实支付网关。


管理后台功能实现总结
一、RBAC 权限体系
新增 4 张数据库表（SQL 脚本在 rbac_init.sql）：
sys_role - 角色表（预置4个角色：超级管理员、演唱会管理员、订单管理员、数据查看员）
sys_permission - 权限表（预置28项权限，覆盖演唱会/场次/场馆/艺人/票种/订单/用户/看板）
sys_role_permission - 角色-权限关联表
sys_user_role - 用户-角色关联表
修改的安全层文件：
LoginUser.java - 新增 permissions 和 roles 字段，getAuthorities() 返回权限+角色集合，增加 hasRole()/hasPermission() 方法
UserDetailsServiceImpl.java - 登录时从 RBAC 表加载角色和权限
SecurityConfig.java - /api/admin/** 路径要求 ROLE_ADMIN 角色
二、管理端 API（全部在 /api/admin/ 路径下）
模块
路径前缀
功能
演唱会管理
/api/admin/concert
分页列表（名称/状态筛选）、详情、新增、编辑、删除
场次管理
/api/admin/show
分页列表（演唱会/状态筛选）、详情、新增、编辑、删除
场馆管理
/api/admin/venue
分页列表（名称/城市筛选）、详情、新增、编辑、删除
艺人管理
/api/admin/artist
分页列表（名称筛选）、详情、新增、编辑、删除
票种&库存
/api/admin/ticket-type
分页列表（场次筛选）、详情、新增、编辑、删除、库存调整
订单管理
/api/admin/order
分页列表（状态/订单号/用户筛选）、详情（含用户+座位）、管理员退款（不受时限）、管理员取消
用户管理
/api/admin/user
分页列表（手机号/状态筛选）、详情（含角色+订单数）、封禁/解封
数据看板
/api/admin/dashboard
销售统计概览（订单数/销售额/退款/活跃数等）、收入报表（按日期范围）
三、使用步骤
执行 src/main/resources/sql/rbac_init.sql 初始化 RBAC 表和数据
先通过前端正常注册一个管理员用户
在数据库中执行 INSERT INTO sys_user_role (user_id, role_id) VALUES (<管理员用户ID>, 1); 为该用户赋予超级管理员角色
使用管理员账号登录后即可访问 /api/admin/** 下所有接口