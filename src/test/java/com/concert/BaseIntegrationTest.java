package com.concert;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 基础集成测试类
 * 所有需要完整 Spring 上下文的测试都应继承此类
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
public abstract class BaseIntegrationTest {
    // 所有集成测试的基类
    // 子类可以直接使用 @Autowired 注入 Bean
}
