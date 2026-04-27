# 单元测试说明

## 目录结构

```
src/test/
├── java/com/concert/
│   ├── BaseIntegrationTest.java          # 基础集成测试类
│   ├── controller/                        # Controller 层测试
│   │   └── UserControllerTest.java
│   ├── service/                           # Service 层测试
│   │   └── UserServiceTest.java
│   ├── utils/                             # 工具类测试
│   │   └── JwtUtilTest.java
│   ├── mapper/                            # Mapper 层测试
│   ├── config/                            # 配置类测试
│   └── common/                            # 通用类测试
└── resources/
    └── application-test.yml               # 测试环境配置
```

## 运行测试

### 使用 Maven 命令

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=UserControllerTest

# 运行特定测试方法
mvn test -Dtest=UserControllerTest#testLoginWithValidCredentials

# 生成测试报告
mvn surefire-report:report
```

### 使用 IDE

- **IntelliJ IDEA**: 右键点击测试类或测试方法，选择 "Run 'xxxTest'"
- **Eclipse**: 右键点击测试类或测试方法，选择 "Run As -> JUnit Test"

## 测试类型

### 1. 单元测试 (Unit Test)
- 测试单个类或方法的功能
- 使用 Mock 对象隔离依赖
- 示例：`JwtUtilTest`

### 2. 集成测试 (Integration Test)
- 测试多个组件的协同工作
- 加载完整的 Spring 上下文
- 示例：`UserControllerTest`（使用 MockMvc）

### 3. Controller 层测试
- 使用 `MockMvc` 模拟 HTTP 请求
- 测试 API 接口的正确性
- 验证请求验证、响应格式等

### 4. Service 层测试
- 测试业务逻辑
- 可以使用 `@MockBean` 模拟依赖
- 测试各种业务场景

## 编写测试的最佳实践

1. **测试命名**：使用 `test[MethodName]_[Scenario]` 格式
   - 示例：`testLoginWithValidCredentials`

2. **Given-When-Then 模式**：
   ```java
   @Test
   void testExample() {
       // 给定 (Given)
       Long userId = 1L;
       
       // 当 (When)
       User user = userService.findById(userId);
       
       // 则 (Then)
       assertNotNull(user);
       assertEquals(userId, user.getId());
   }
   ```

3. **测试独立性**：每个测试应该独立运行，不依赖其他测试

4. **测试覆盖**：
   - 正常流程测试
   - 边界条件测试
   - 异常场景测试

5. **使用断言**：
   - `assertEquals` - 验证相等
   - `assertNotNull` - 验证非空
   - `assertTrue/False` - 验证布尔值
   - `assertThrows` - 验证异常

## 测试配置

测试环境使用 `src/test/resources/application-test.yml` 配置文件，与开发环境隔离。

### 注意事项

1. **数据库**：建议使用测试数据库，避免污染开发数据
2. **Redis**：使用不同的数据库索引（如 database: 1）
3. **外部服务**：短信服务等外部依赖应该使用 Mock
4. **测试数据**：测试完成后应清理测试数据，或使用事务回滚

## 常用注解

- `@Test` - 标记测试方法
- `@SpringBootTest` - 加载完整 Spring 上下文
- `@AutoConfigureMockMvc` - 配置 MockMvc
- `@Autowired` - 注入 Bean
- `@MockBean` - 创建 Mock Bean
- `@BeforeEach` - 每个测试前执行
- `@AfterEach` - 每个测试后执行
- `@Disabled` - 跳过测试

## 下一步

1. 为所有 Service 实现类编写测试
2. 为所有 Controller 编写测试
3. 为工具类编写测试
4. 为 Mapper 编写测试（测试 SQL 查询）
5. 配置 CI/CD 自动运行测试
