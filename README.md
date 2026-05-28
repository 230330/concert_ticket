这是一个基于 Spring Boot 2.7 + MyBatis-Plus + Spring Security + Redis + RabbitMQ 的演唱会订票系统，采用经典的分层架构，前端使用 Vue 3 + Vite + Element Plus + Pinia。以下是各层的具体分析：

整体项目结构：
```
com.concert
├── ConcertTicketApplication.java   # 启动类
├── annotation/                     # 自定义注解（@RateLimit 限流注解）
├── aspect/                         # AOP 切面（RateLimitAspect 限流切面）
├── common/                         # 通用层（Result<T> 统一响应封装）
├── config/                         # 配置层（含 security 子包）
├── controller/                     # 控制器层（含 admin 子包）
├── dto/                            # 数据传输对象（request / response）
├── entity/                         # 实体层
├── enums/                          # 枚举层
├── exception/                      # 异常层
├── mapper/                         # 持久层
├── mq/                             # 消息队列（MessageProducer / MessageConsumer）
├── service/                        # 服务层（含 concert / order / impl 子包）
├── task/                           # 定时任务层
└── utils/                          # 工具层
```

1. common — 通用响应封装
   只有一个 Result<T> 类，是全局统一的 API 响应格式，包含 code、message、data 三个字段。提供了 success()、error()、unauthorized()、forbidden() 等静态工厂方法，被所有 Controller 和安全异常处理器使用，确保接口返回格式一致。

2. annotation — 自定义注解
   @RateLimit：接口限流注解，支持 key（限流标识）、count（次数上限）、period（时间窗口/秒）、limitType（USER/IP 两种模式）。标记在 Controller 方法上，由 RateLimitAspect 切面拦截处理，基于 Redis INCR + EXPIRE 实现固定窗口限流，超限抛出 BusinessException("操作过于频繁，请稍后再试")。

3. aspect — AOP 切面
   RateLimitAspect：限流切面，处理 @RateLimit 注解。从 SecurityContext 获取用户ID（USER 模式）或从 HttpServletRequest 获取客户端IP（IP 模式）作为限流标识，使用 StringRedisTemplate.opsForValue().increment() + expire 实现固定窗口计数，超限抛出 BusinessException。

4. config — 配置层
   这一层负责框架级别的基础设施配置，分为十个部分：

   MybatisPlusConfig：注册分页插件 PaginationInnerInterceptor（演唱会列表分页查询依赖它）以及自动填充处理器，在数据插入/更新时自动写入 createTime 和 updateTime。

   RedisConfig：配置 RedisTemplate<String, Object>，使用 Jackson 序列化 value，支持 Java 8 时间类型。主要被短信验证码服务、分布式锁和缓存管理使用。

   CacheConfig：Spring Cache + Redis 缓存管理器配置，启用 @EnableCaching。定义了 4 个缓存空间——concert:list（10分钟）、concert:detail（30分钟）、show:info（5分钟）、concert:hot（5分钟），使用 Jackson2JsonRedisSerializer 序列化并禁用 null 值缓存。对高频访问的演唱会列表、详情、场次信息、热门推荐接口提供缓存策略。

   RabbitMQConfig：RabbitMQ 交换机、队列和绑定关系配置。定义了 3 个交换机——concert.exchange.topic（Topic 类型，用于短信和支付通知）、concert.exchange.delay（Direct 类型，用于订单超时延迟消息）、concert.exchange.dlx（Direct 类型，死信交换机）。定义了 5 个队列——concert.queue.sms（短信发送）、concert.queue.payment（支付通知）、concert.queue.order.timeout（订单超时处理）、concert.queue.order.timeout.delay（基于 TTL + 死信转发的延迟队列）、concert.queue.dlx（死信队列，消费失败消息的兜底处理）。所有业务队列配置死信转发到 DLX。

   RabbitMQConfirmConfig：RabbitMQ 消息可靠性保证配置，实现 RabbitTemplate.ConfirmCallback 和 ReturnsCallback。@PostConstruct 设置回调——消息到达交换机时记录日志，路由失败时记录 warn 日志并触发 ReturnsCallback。

   AlipayConfig + AlipayProperties：支付宝 SDK 集成配置。AlipayProperties 通过 @ConfigurationProperties(prefix = "alipay") 绑定配置项（appId、privateKey、publicKey、gateway、signType、charset、format、notifyUrl、returnUrl），AlipayConfig 初始化 DefaultAlipayClient Bean。

   JacksonConfig：全局 Jackson 配置，统一 LocalDateTime 序列化/反序列化格式为 yyyy-MM-dd HH:mm:ss，禁用时间戳格式。

   WebMvcConfig：配置静态资源映射，使上传的头像文件可通过 URL 直接访问。

   security 子包（共 6 个类）：这是整个认证鉴权体系的核心。SecurityConfig 定义了过滤链——关闭 CSRF、启用 CORS、无状态会话、@EnableGlobalMethodSecurity 启用方法级权限控制、公开接口白名单（登录/注册/短信/演唱会浏览/支付宝回调等），管理端接口 /api/admin/** 需要 ROLE_ADMIN 角色，其余接口均需认证。

   JwtAuthenticationTokenFilter 是自定义过滤器，拦截每个请求，从 Authorization 头提取 JWT、验证有效性、加载用户信息到 SecurityContext。LoginUser 实现了 UserDetails，以手机号作为登录标识，并新增了 permissions 和 roles 字段支持 RBAC 权限判断（hasRole()/hasPermission() 方法）。

   UserDetailsServiceImpl 通过 UserMapper 按手机号查库构建 LoginUser，同时从 RBAC 表加载角色和权限。AccessDeniedHandlerImpl 和 AuthenticationEntryPointImpl 分别处理 403 和 401 的 JSON 响应。

5. controller — 控制器层（用户端）
   共 8 个用户端控制器 + 8 个管理端控制器（在 admin 子包中）。用户端负责接收 HTTP 请求、参数校验、调用 Service 并组装返回数据：

   ConcertController (/api/concert)：提供热门演唱会分页列表、即将开演列表、演唱会详情（含艺人、场次信息）。热门和搜索接口分别添加了 @RateLimit(key="concert:hot", count=30, period=60, limitType=IP) 和 @RateLimit(key="concert:search", count=20, period=60, limitType=IP) 限流保护。

   ShowController (/api/show)：查询某演唱会下的场次列表（含票价、库存、区域信息），以及场次座位图（按区域→排→座组织，标记已售状态）。

   OrderController (/api/order)：需登录访问，提供创建订单、模拟支付、取消订单、退款订单、查看订单详情、我的订单列表等接口。创建订单和支付接口分别添加了 @RateLimit(key="order:create", count=5, period=60) 和 @RateLimit(key="order:pay", count=5, period=60) 限流保护，防止恶意刷单。

   PaymentController (/api/pay)：支付宝支付控制器，提供 4 个接口——POST /alipay/create（发起支付，需登录，@RateLimit 5/60s）、POST /alipay/notify（异步通知回调，公开接口，支付宝服务器主动调用）、GET /alipay/return（同步跳转，公开接口）、GET /alipay/status（查询支付状态，@RateLimit 20/60s）。创建支付时验证订单归属和状态，异步回调通过 AlipaySignature.rsaCheckV1 验签后更新订单。

   UserController (/api/user)：提供登录、注册（含短信验证码校验）、发送验证码、重置密码、修改密码、查看/修改个人信息等接口。

   SmsController (/api/sms)：独立的短信服务入口，提供发送验证码和校验验证码接口。

   TicketController (/api/ticket)：取票码相关，提供获取当前用户取票码列表和线下核销取票码接口。

   FileUploadController (/api/file)：文件上传，提供头像上传接口，支持 JPG/PNG 格式，限制 10MB。

6. dto — 数据传输对象
   分为 request 和 response 两个子包，起到前后端数据隔离的作用：

   Request（19 个）：包括 CreateOrderRequest（showId + ticketTypeId + seatIds）、LoginRequest/RegisterRequest/SendSmsRequest（手机号正则校验 ^1[3-9]\d{9}$）、PayOrderRequest/CancelOrderRequest/RefundOrderRequest（orderId）、UserUpdateRequest（昵称/头像/邮箱，均可选）、ChangePasswordRequest/ResetPasswordRequest（密码修改）、ConcertRequest/ShowRequest/VenueRequest/ArtistRequest/TicketTypeRequest（管理端 CRUD 请求）、ConcertStatusRequest/ShowStatusRequest/UserStatusRequest（状态更新）、StockAdjustRequest（库存调整）。均使用 @Valid 注解做参数校验。

   Response（12 个）：ConcertListResponse/ConcertDetailResponse（演唱会摘要和详情，详情含嵌套的 ArtistInfo 和 ShowInfo 内部类）、ShowListResponse（含票种信息的场次列表）、SeatMapResponse（多层嵌套：区域→排→座位，含 sold 标志）、OrderResponse（含 statusDesc 状态描述映射和 SeatDetail 座位明细）、LoginResponse（token + 过期时间）、UserInfoResponse（含角色列表）、PageResponse<T>（通用分页包装）、AdminOrderResponse（管理端订单详情，含用户+座位信息）、AdminUserResponse（管理端用户详情，含角色+订单数）、DashboardSalesResponse（销售概览）、DashboardRevenueResponse（收入报表）。

7. entity — 实体层
   共 16 个实体，对应数据库表，使用 Lombok @Data 和 MyBatis-Plus 注解。核心关系是：

   User 一对多 Order，Order 一对多 OrderSeat，OrderSeat 关联 Seat。Concert 多对多 Artist（通过 ConcertArtist 中间表），Concert 一对多 Show，Show 属于 Venue，Venue 一对多 SeatArea 一对多 Seat，Show 一对多 TicketType（每种票种关联一个 SeatArea）。

   新增实体：SmsVerificationCode（短信验证码记录，替代 Redis 存储验证码，支持过期和已使用标记）、SysRole（角色）、SysPermission（权限）、SysRolePermission（角色-权限关联）、SysUserRole（用户-角色关联），共同构成 RBAC 权限体系。

   关键状态字段：Concert.status（0 未开始 / 1 进行中 / 2 已结束 / 3 已取消）、Show.status（0 未开售 / 1 售票中 / 2 售罄 / 3 已结束 / 4 已取消）、Order.status（0 待支付 / 1 已支付 / 2 已取消 / 3 已退款 / 4 已完成）。

8. enums — 枚举层
   共 5 个枚举，代码中所有状态字段均已使用枚举替代魔术数字：

   ConcertStatus（NOT_STARTED / IN_PROGRESS / ENDED / CANCELLED）— 演唱会生命周期状态。
   ShowStatus（NOT_ON_SALE / ON_SALE / SOLD_OUT / ENDED / CANCELLED）— 场次售票状态。
   OrderStatus（PENDING / PAID / CANCELLED / REFUNDED / COMPLETED）— 订单生命周期状态。
   SeatStatus（可用 / 占用中 / 锁定中）— 座位实时状态。
   UserStatus（DISABLED / NORMAL）— 用户账号状态。

   所有枚举均使用 @EnumValue 注解映射数据库数值，@JsonValue/@JsonCreator 确保前后端 JSON 序列化/反序列化一致。

9. exception — 异常层
   建立了一套完整的自定义异常体系，包含 BusinessException（业务异常）、NotFoundException（资源未找到）、ForbiddenException（无权限操作）、UnauthorizedException（未授权访问），以及全局异常处理器 GlobalExceptionHandler。

   GlobalExceptionHandler 使用 @RestControllerAdvice 统一处理各类异常：BusinessException 返回业务错误提示、NotFoundException 返回 404、ForbiddenException 返回 403、MethodArgumentNotValidException 和 BindException 返回参数校验错误（400）、DuplicateKeyException 返回数据唯一键冲突（409）、Exception 兜底处理未捕获异常并生成错误 ID（500），绝不向前端暴露内部堆栈信息。

10. mapper — 持久层
    共 16 个 Mapper 接口，全部继承 MyBatis-Plus 的 BaseMapper<T>。SQL 定义采用 XML 映射文件（resources/mapper/），状态值通过 OGNL 引用枚举编译期常量，避免硬编码魔法数字。核心操作如扣减库存的条件更新使用自定义 SQL（available_stock >= N 原子判断）。

11. mq — 消息队列层
    MessageProducer：消息生产者，提供 3 个发送方法——sendSmsMessage（短信发送，路由到 topic exchange）、sendPaymentNotification（支付通知，路由到 topic exchange）、sendOrderTimeoutMessage（订单超时延迟消息，发送到延迟队列，基于 TTL + 死信转发实现，支持自定义延迟时间）。

    MessageConsumer：消息消费者，包含 4 个消费者方法。全部采用手动 ACK（channel.basicAck/basicNack），格式错误的消息直接丢弃不重试：
    - handleSmsMessage：处理短信发送消息，调用 NotificationService 发送短信
    - handlePaymentMessage：处理支付通知消息，记录支付成功日志
    - handleOrderTimeoutMessage：处理订单超时消息，调用 orderService.cancelExpiredOrder 自动取消过期订单
    - handleDlxMessage：处理死信消息，记录消费失败的兜底日志，可用于人工补偿

12. service — 服务层
    按业务领域拆分为三个子包，遵循单一职责原则：

    concert 子包（3 个接口 + 3 个实现）：
    - ConcertCoreService：演唱会核心 CRUD，getConcertDetail() 添加 @Cacheable(value="concert:detail") 缓存
    - ConcertSearchService：演唱会搜索，getHotConcerts() 添加 @Cacheable(value="concert:hot") 缓存，searchConcerts() 添加 @Cacheable(value="concert:list") 缓存
    - ConcertShowService：场次相关查询，getUpcomingConcerts() 添加 @Cacheable(value="show:info") 缓存
    - ConcertResponseAssembler：演唱会响应组装器，负责将多个实体组装为前端需要的 DTO

    order 子包（4 个接口 + 4 个实现）：
    - OrderCoreService：订单核心流程（创建订单）。createOrder 使用分布式锁（DistributedLock.tryLockWithRetry）保护库存扣减防止超卖，事务内完成校验→创建订单→锁定座位→扣减库存→发送MQ延迟消息（messageProducer.sendOrderTimeoutMessage）实现订单自动取消
    - OrderPaymentService：订单支付（模拟支付，生成取票码）
    - OrderCancellationService：订单取消/退款/批量过期处理/批量完成处理
    - OrderQueryService：订单查询（详情、列表、取票码查询）

    impl 子包（18 个实现）：大部分是空壳（继承 ServiceImpl<Mapper, Entity> 即可），核心业务逻辑已拆分到上述子包中。其他重要实现：

    PaymentServiceImpl — 支付宝支付服务实现。createAlipayOrder 使用 Redis SETNX 实现支付幂等性（防重复提交），调用 AlipayTradePagePayRequest 生成支付表单 HTML。handleAlipayNotify 通过 AlipaySignature.rsaCheckV1 验签后根据 TRADE_SUCCESS/TRADE_FINISHED 更新订单状态。alipayRefund 调用 AlipayTradeRefundRequest 执行退款。queryAlipayTrade 调用 AlipayTradeQueryRequest 查询交易状态。

    SmsVerificationCodeServiceImpl — 使用数据库表 SmsVerificationCode 存储验证码，生成 6 位安全随机数字码（SecureRandomUtil），5 分钟过期，发送时将同一手机号之前未过期的验证码标记为已使用，验证成功后标记为已使用防止重复利用。新增 sendVerificationCodeSms 方法供 MQ 消费者调用。

    NotificationService — 异步通知服务，使用 @Async 注解，用于支付成功后异步发送取票码短信通知。

13. task — 定时任务层
    OrderCleanupTask：包含两个独立定时任务：
    ① 每 15 分钟执行一次（cron = "0 */15 * * * ?"），分批查询过期未支付订单（每批最多 100 条），批量取消并释放座位和库存。使用 Redis 分布式锁防止多实例重复执行。
    ② 每小时整点执行一次（cron = "0 0 */1 * * ?"），将演出时间已过的已支付订单自动标记为已完成（每批 500 条），同样使用 Redis 分布式锁。

14. utils — 工具层
    JwtUtil：从 application.yml 读取 JWT 密钥和过期时间（24 小时），基于 HMAC-SHA256 算法签发和验证 token，携带 userId 和 phone 两个 claim。

    PageUtil：分页参数校验工具，校验 page/size 参数合法性，支持自动设置默认值和上限截断。

    SecureRandomUtil：安全随机数工具，基于 SHA1PRNG 算法生成纯数字验证码、字母数字混合取票码、Base64 URL-Safe 随机字符串。

    SecurityUtil：安全上下文工具类，提供 getCurrentUserId() 和 getCurrentUserPhone() 静态方法，封装从 SecurityContextHolder 提取当前登录用户信息的逻辑。

    DistributedLock：Redis 分布式锁工具类，基于 SETNX + Lua 脚本实现。提供 tryLock()（单次尝试）、tryLockWithRetry()（带重试，支持自定义重试次数和间隔）、unlock()（Lua 脚本安全释放，防止误删其他线程的锁）。锁值使用 UUID 防止误删，提供 executeWithLock() 和 executeWithLockRetry() 便捷方法。被 OrderCoreServiceImpl 用于库存扣减的并发保护。

---

核心业务流程

一、用户认证流程
整个认证体系围绕手机号 + 密码 + 短信验证码构建，采用无状态 JWT 机制。

注册：调用 /api/user/sendSms 请求短信验证码，系统生成 6 位安全随机码存入 SmsVerificationCode 表（5 分钟过期）。用户携带手机号、密码和验证码调用 /api/user/register，后端先通过 SmsVerificationCodeService.verifyCode() 校验验证码（匹配后立即标记已使用），再检查手机号是否已注册，最后将密码 BCrypt 加密后存入数据库。

登录：调用 /api/user/login，Spring Security 的 AuthenticationManager 负责验证手机号和密码。验证通过后 JwtUtil 签发一个携带 userId 和 phone 的 JWT（有效期 24 小时），返回给前端。之后每次请求，JwtAuthenticationTokenFilter 从请求头提取 token、校验有效性、加载用户信息到 SecurityContext，完成无状态认证。

忘记密码：调用 /api/user/sendResetSms 先校验手机号已注册且状态正常，再发送验证码；然后调用 /api/user/resetPassword 提交新密码完成重置。

修改密码（已登录）：调用 /api/user/changePassword，需要提供原密码验证身份。

二、演唱会浏览流程
这条线路不需要登录（在 SecurityConfig 中配置为 permitAll），是面向所有访客的公开接口。热门演唱会列表和搜索接口通过 Redis 缓存（@Cacheable）加速响应，同时通过 @RateLimit 限流保护。用户可以浏览热门演唱会列表（GET /api/concert/hot）、即将开演的演唱会列表（GET /api/concert/upcoming）、演唱会详情（GET /api/concert/{id}，30分钟缓存）、场次票种信息（GET /api/show/list）和座位图（GET /api/show/{showId}/seats）。

三、下单购票流程（核心）
这是系统最关键的业务环节，需要登录且在单个数据库事务内完成，集中体现在 OrderCoreServiceImpl.createOrder() 中。用户选好场次、票种和座位后，提交 POST /api/order/create，后端按以下步骤处理：

第一步，业务校验：验证场次存在且状态为"售票中"；验证票种存在且属于该场次；验证选座数量不超过 4 张（单笔限购）；验证剩余库存大于等于选座数量；验证所有座位真实存在且属于该票种对应的区域。

第二步，创建订单：生成 UUID 格式的 20 位订单号，计算总金额（单价 × 座位数），设置订单状态为待支付，过期时间为当前时间 + 15 分钟。

第三步，锁定座位：批量插入 OrderSeat 记录。利用数据库唯一索引 (show_id, seat_id) 防止并发选座冲突——后提交的触发 DuplicateKeyException，系统捕获后返回"座位已被占用"。

第四步，扣减库存（分布式锁保护）：通过 DistributedLock.tryLockWithRetry("stock:" + ticketType.getId()) 获取分布式锁，在锁保护下执行条件 SQL UPDATE ticket_type SET available_stock = available_stock - N WHERE id = ? AND available_stock >= N 原子扣减库存。如果受影响行数为 0 说明库存不足，事务回滚。锁释放通过 Lua 脚本保证安全（防止误删其他线程的锁）。

第五步，发送订单超时延迟消息：订单创建成功后，通过 MessageProducer.sendOrderTimeoutMessage(orderId, delayMs) 发送延迟消息到 RabbitMQ 延迟队列，消息在 orderExpireMinutes × 60 × 1000 毫秒后自动转发到订单超时处理队列，由 MessageConsumer 消费并调用 cancelExpiredOrder 自动取消过期订单。

四、支付流程（支付宝集成）
用户在 15 分钟内调用 POST /api/pay/alipay/create 发起支付，后端验证订单归属和状态后调用 PaymentServiceImpl.createAlipayOrder()，该方法先通过 Redis SETNX（key = pay:idempotent:{orderId}）实现支付幂等性检查（防止重复提交），然后构建 AlipayTradePagePayRequest 调用支付宝电脑网站支付 API，返回支付表单 HTML 供前端跳转。用户在支付宝完成支付后，支付宝服务器异步调用 POST /api/pay/alipay/notify，后端通过 AlipaySignature.rsaCheckV1 验证签名后根据交易状态（TRADE_SUCCESS/TRADE_FINISHED）更新订单为已支付并生成取票码。前端可通过 GET /api/pay/alipay/status 轮询支付状态。

五、退款流程
已支付订单的买家可在规定时效内调用 PUT /api/order/refund 申请退款。如已对接支付宝，可通过 PaymentServiceImpl.alipayRefund() 调用 AlipayTradeRefundRequest 向支付宝发起退款请求。管理端可进行不受时限的强制退款。

六、取票核验流程
支付成功后生成的取票码可通过 GET /api/ticket/my-codes 查询当前用户的取票码列表；线下工作人员通过 POST /api/ticket/verify?pickupCode=xxx 核销取票码，核销后订单状态更新为已完成。

七、我的订单
用户可通过 GET /api/order/my?status=&page=&size= 分页查询自己的订单，支持按状态筛选（待支付/已支付/已取消/已退款/已完成）。

---

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
SecurityConfig.java - /api/admin/** 路径要求 ROLE_ADMIN 角色，开启 @EnableGlobalMethodSecurity(prePostEnabled = true)

二、管理端 API（全部在 /api/admin/ 路径下）
模块 | 路径前缀 | 功能
演唱会管理 | /api/admin/concert | 分页列表（名称/状态筛选）、详情、新增、编辑、删除
场次管理 | /api/admin/show | 分页列表（演唱会/状态筛选）、详情、新增、编辑、删除
场馆管理 | /api/admin/venue | 分页列表（名称/城市筛选）、详情、新增、编辑、删除
艺人管理 | /api/admin/artist | 分页列表（名称筛选）、详情、新增、编辑、删除
票种&库存 | /api/admin/ticket-type | 分页列表（场次筛选）、详情、新增、编辑、删除、库存调整
订单管理 | /api/admin/order | 分页列表（状态/订单号/用户筛选）、详情（含用户+座位）、管理员退款（不受时限）、管理员取消
用户管理 | /api/admin/user | 分页列表（手机号/状态筛选）、详情（含角色+订单数）、封禁/解封
数据看板 | /api/admin/dashboard | 销售统计概览（订单数/销售额/退款/活跃数等）、收入报表（按日期范围）

---

技术架构总结

一、后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.x | 应用框架 |
| MyBatis-Plus | 3.5.x | ORM 框架 |
| Spring Security | 2.7.x | 认证鉴权 |
| Redis | - | 缓存 + 限流 + 分布式锁 |
| RabbitMQ | - | 异步消息队列 |
| 支付宝 SDK | 4.38.x | 支付集成 |
| MySQL | 8.0 | 关系型数据库 |
| JWT | - | 无状态认证 |
| SpringDoc | 1.7.x | API 文档（Swagger 3） |
| Lombok | - | 代码简化 |

二、前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.3.8 | 前端框架（Composition API + \<script setup\>） |
| Vite | 5.0.0 | 构建工具（替代 Vue CLI） |
| Element Plus | 2.4.0 | UI 组件库（替代 Element UI） |
| Vue Router | 4.2.5 | 路由管理（createRouter + addRoute） |
| Pinia | 2.1.7 | 状态管理（替代 Vuex） |
| Axios | 1.6.0 | HTTP 客户端 |

三、Redis 在系统中的三种角色
1. **缓存**：通过 Spring Cache + @Cacheable 注解对高频访问的演唱会列表（10min）、详情（30min）、场次信息（5min）、热门推荐（5min）进行缓存，减少数据库压力
2. **限流**：通过 @RateLimit 注解 + RateLimitAspect 切面，基于 Redis INCR + EXPIRE 实现固定窗口限流，支持按用户ID或IP限流，保护关键接口（下单5次/分钟、支付5次/分钟、热门30次/分钟、搜索20次/分钟）
3. **分布式锁**：通过 DistributedLock 工具类，基于 SETNX + Lua 脚本实现，用于库存扣减的并发保护，防止超卖

四、RabbitMQ 消息架构
```
                    ┌─────────────┐
                    │ Topic Exchange │
                    │ concert.exchange.topic │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            │
     concert.queue.sms  concert.queue.payment
     (短信发送)          (支付通知)        │
              │            │            │
              ▼            ▼            │
        MessageConsumer  MessageConsumer│
                                       │
                    ┌─────────────┐    │
                    │ Direct Exchange │   │
                    │ concert.exchange.delay │
                    └──────┬──────┘    │
                           │            │
                    ┌──────▼──────┐    │
                    │ Delay Queue  │    │
                    │ concert.queue.order.timeout.delay │
                    │ (TTL + DLX)  │    │
                    └──────┬──────┘    │
                           │ 过期转发    │
                    ┌──────▼──────┐    │
                    │ Timeout Queue│   │
                    │ concert.queue.order.timeout │
                    └──────┬──────┘    │
                           │            │
                    ┌──────▼──────┐    │
                    │ DLX Exchange  │◄───┘ (所有队列的死信转发)
                    │ concert.exchange.dlx │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Dead Letter Queue │
                    │ concert.queue.dlx │
                    └─────────────┘
```

消息可靠性保证：
- **生产者确认**：Publisher Confirm（correlated 模式）+ Publisher Return，消息到达交换机和路由失败均有回调
- **消费确认**：手动 ACK（channel.basicAck/basicNack），格式错误消息直接丢弃不重试，业务异常消息可重试
- **消费重试**：开启 Spring AMQP 重试（max-attempts=3, initial-interval=1000ms）
- **死信队列**：所有业务队列配置死信转发到 DLX，消费失败的消息进入死信队列兜底处理

五、支付宝支付集成
- **支付方式**：电脑网站支付（Page Pay），返回表单 HTML 前端直接提交跳转
- **安全机制**：RSA2 签名验签、Redis SETNX 幂等性检查（防重复提交）
- **回调处理**：异步通知（/api/pay/alipay/notify）验签后更新订单状态，同步跳转（/api/pay/alipay/return）供前端展示结果
- **退款**：调用 AlipayTradeRefundRequest 实现退款
- **状态查询**：前端通过 GET /api/pay/alipay/status 轮询支付结果

---

使用步骤

1. 执行 src/main/resources/sql/ 下的 SQL 脚本初始化数据库和 RBAC 数据
2. 安装并启动 Redis、RabbitMQ 服务
3. 修改 application.yml 中的数据库、Redis、RabbitMQ、支付宝等配置（生产环境通过环境变量注入）
4. 启动后端服务：mvn spring-boot:run
5. 进入 concert-ticket-frontend 目录，执行 npm install && npm run dev 启动前端
6. 先通过前端正常注册一个管理员用户
7. 在数据库中执行 INSERT INTO sys_user_role (user_id, role_id) VALUES (<管理员用户ID>, 1); 赋予超级管理员角色
8. 使用管理员账号登录后即可访问 /api/admin/** 下所有接口
9. API 文档访问：http://localhost:8090/swagger-ui.html
