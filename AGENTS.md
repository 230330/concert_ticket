# AGENTS.md

本文件记录本仓库的工程约定，供在本项目工作的开发者与 AI 助手共同遵循。
目录结构、业务流程与接口清单见 README.md；改动涉及结构时请同步更新 README。

## 项目概览

- 后端：Spring Boot 2.7.18 / Java 8 / MyBatis-Plus 3.5.5 / MySQL / Redis / Spring Security + JWT / 支付宝
- 消息队列：RabbitMQ，默认关闭（`concert.mq.enabled=false`）
- 前端：Vue 3 + Vite 5 + Element Plus + Pinia，位于 `concert-ticket-frontend/`

## 常用命令

| 目的 | 命令 |
|------|------|
| 编译 | `mvn -DskipTests compile` |
| 测试（必须全绿） | `mvn test` |
| 全量构建 | `mvn verify` |
| 启动后端 | `mvn spring-boot:run` |
| 前端安装依赖 | `cd concert-ticket-frontend && npm install` |
| 前端代码检查（必须通过） | `cd concert-ticket-frontend && npm run lint` |
| 前端构建 | `cd concert-ticket-frontend && npm run build` |
| 启动前端开发服务 | `cd concert-ticket-frontend && npm run dev` |

测试不得依赖 MySQL、Redis、RabbitMQ 等外部环境，必须能在裸环境下直接运行。
CI（`.github/workflows/ci.yml`）会并行执行后端 `mvn verify` 与前端 `npm run lint` + `npm run build`，提交前请本地先跑通对应命令。

## 前端代码风格

1. eslint 配置在 `concert-ticket-frontend/.eslintrc.js`，解析链使用 eslint 自带的 espree 与 vue-eslint-parser，**不要**改回 `@babel/eslint-parser`（会读取仍引用 Vue CLI preset 的 `babel.config.js` 导致全量解析失败）。
2. 提交前必须保证 `npm run lint` 无输出通过。
3. `src/` 下新增接口调用统一走 `src/api/`，不要在页面里直接调 `request`。
4. 状态映射统一从 `@/constants/status` 导入（订单/演唱会/场次/用户四套，含 `resolveStatusLabel`、`resolveStatusTagType`、`toStatusOptions`），不要各页面再复制一份，也不要在模板里散写状态值与文案。
5. Element Plus 图标用组件形式：模板内 `<el-icon><Search /></el-icon>`，或作为属性传组件名 `<el-button icon="Search" />`（EP2 通过 resolveDynamicComponent 解析名字，字符串即可）。
   新增图标登记在 `src/icons/element-plus.js`（main.js 统一全局注册）。**不要**再使用 `el-icon-xxx` 这类字体图标，EP2 已移除、写了也不显示。
6. 定时器、事件监听等清理逻辑必须写在 `beforeUnmount` 里。Vue 3 没有 `beforeDestroy`，写了不会执行，定时器与监听会泄漏。

### Vue 2 → Vue 3 迁移检查清单

以下写法在 Vue 3 中**静默失效**（不报错、但功能不可用），改动模板或页面时对照检查：

| 失效写法 | 正确写法 | 失效后果 |
|----------|----------|----------|
| `slot-scope="{row}"` | `#default="{ row }"` | 内容被塞进不渲染的 `<template>`，表格自定义列（状态标签、操作按钮）全都不显示 |
| `slot="header"` / `slot="footer"` | `#header` / `#footer` | 具名插槽未填充，卡片头部与弹窗按钮不显示 |
| `:visible.sync="x"` | `v-model="x"` | EP2 对话框没有 `visible` prop，弹窗打不开 |
| `:current-page.sync="x"` | `v-model:current-page="x"` | 分页页码无法回写 |
| `@click.native` / `@keyup.enter.native` | `@click` / `@keyup.enter` | 监听器不触发（如退出登录、注册按钮无响应） |
| `beforeDestroy` | `beforeUnmount` | 清理逻辑不执行 |
| `el-submenu`（Element UI） | `el-sub-menu`（Element Plus 2） | 组件不存在，多级菜单不渲染 |
| `size="mini"` | `size="small"` | 不是合法尺寸值 |

排查顺序建议：先用 `npm run lint` + `npm run build` 过一遍，再对上述写法各 grep 一次（除本文档与 `src/icons/element-plus.js` 注释里的说明文字外，应全部为 0）。

## 后端代码风格

1. 类头使用中文 Javadoc：`@description` / `@author` / `@date`。
2. 依赖注入优先 `@Resource`；只有需要 `required = false` 时才用 `@Autowired`。
3. 长方法用 `// 1. xxx` `// 2. xxx` 编号注释分步。
4. Controller 只做参数校验、取当前用户、调用 Service、包装 Result，不写业务逻辑。
5. 统一响应体使用 `Result` 的静态工厂方法；业务异常抛自定义异常，交给 `GlobalExceptionHandler` 统一处理。
6. 接口返回语义：业务错误与参数校验错误返回 **HTTP 200 + 响应体 code**（前端按 code 判断）；仅安全相关错误（401/403）由 Spring Security 的处理器设置真实 HTTP 状态码。新增接口不要破坏这一约定。
7. 枚举必须带 `@EnumValue` + `@JsonValue` + `@JsonCreator`，并提供 `XXX_VALUE` 编译期常量，供注解里的 SQL 引用（注解属性不能用方法调用）。
8. 配置一律外部化：密钥类用 `${ENV_VAR:默认值}`，业务阈值放 `concert.*` 并用 `@Value` 读取。
9. 业务错误按语义抛 `BusinessException` / `NotFoundException` / `ForbiddenException`，不要直接抛 RuntimeException。
10. 列表与批量场景不要逐条查库：关联数据用 `listByIds` 批量取后再内存装配，统计类用一条分组 SQL（如 `OrderMapper.countOrdersByUserIds`），禁止 for 循环里执行查询。
11. 认证链每请求都会执行，改动 `UserDetailsServiceImpl` 时注意角色只查一次并复用给权限计算；不要调用会重复查询角色的 `SysPermissionService.getPermissionCodesByUserId`。

## 缓存约定

- 缓存区域必须在 `CacheConfig` 中声明 TTL。**未声明的区域会静默使用 30 分钟默认 TTL**，`CacheConfigTest` 对此有断言保护。
- 缓存名使用 `CacheConfig` 的常量，不要写字面量。
- 管理端写接口（`/api/admin/**` 下的新增、编辑、状态变更、删除、库存调整）必须加 `@EvictConcertCache`，由 `ConcertCacheEvictionAspect` 统一清理用户端四个缓存区域。
  四个区域的响应字段同时来自演唱会、场次、场馆、艺人、票档五类实体，因此不做按实体的细粒度失效。
- 缓存故障不得影响业务：读写失败由 `LoggingCacheErrorHandler` 降级（读取回落数据库），注册方式见 `CacheConfig`，注意直接声明 Bean 不会被 Spring 识别。
- 清空缓存走切面而非 `@CacheEvict` 注解，因为需要 "失败只告警" 的语义与集中的区域映射。

## 测试约定

- 单测使用 Mockito。`@InjectMocks` 不会注入 `@Value`、`@Resource` 字段，需要 `ReflectionTestUtils` 显式设值；漏设会让 `@Value` 字段是 `0`/`null`，表现为校验逻辑异常（例如上限判断恒真）。
- Controller 测试只装配 Controller 与 `GlobalExceptionHandler`，业务依赖全部 mock，示例见 `UserControllerTest`。
- **不要用 `@WebMvcTest`**：启动类上的 `@MapperScan` 会连带注册全部 Mapper，缺少 `SqlSessionFactory` 时上下文起不来。
- 修复测试时不得为了让用例变绿而弱化断言。断言与实现不一致时，先判断是测试过期还是实现有 bug，属于实现问题的要单独提出、不要偷偷改测试迁就。
- 调整实现后要同步检查对应单测的 mock 是否还成立（新增依赖会让旧测试 NPE）。

## 安全约定

- 生产部署必须显式声明 `SPRING_PROFILES_ACTIVE=prod`，`ProductionSafetyValidator` 会校验 JWT 密钥、数据库密码、CORS 来源、模拟支付开关与支付宝配置，不合规直接中断启动。
- 模拟支付 `concert.order.mock-pay-enabled` 仅用于开发/测试，默认关闭；生产必须为 `false`（前端需走支付宝通道）。
- 新增管理端写接口时，除缓存失效外还要确认权限（`/api/admin/**` 已要求 ROLE_ADMIN）。
- 越权风险的接口（如取票码核销）必须在 `SecurityConfig` 中显式限定角色，不要依赖 `anyRequest().authenticated()`。
- 密钥、密码等敏感配置不得硬编码，统一走环境变量。

## 提交约定

- 提交信息格式：`@hzf@<中文摘要>`，正文分段说明「问题 / 方案 / 自测结果 / 未覆盖项」。
- 提交前必须执行 `mvn test` 并保证全绿。
- 一个提交只做一件事，便于回溯与回滚。
