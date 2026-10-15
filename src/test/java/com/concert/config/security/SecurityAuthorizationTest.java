package com.concert.config.security;

import com.concert.enums.UserStatus;
import com.concert.service.OrderService;
import com.concert.service.concert.ConcertShowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 授权规则测试
 *
 * <p>锁定 SecurityConfig 中的三条规则，防止被后续改动破坏：
 * 1) /api/admin/** 需要 ADMIN 角色；
 * 2) /api/ticket/verify（取票码核销）需要 ADMIN 角色——普通登录用户曾可直接核销他人取票码；
 * 3) 用户端需要登录、演唱会浏览等公开接口无需登录。
 *
 * <p>用 authentication() 直接注入带真实 LoginUser 的认证信息（@WithMockUser 的 principal 不是 LoginUser，
 * 会让 SecurityUtil 取当前用户时报类型转换错误），并 mock 掉业务依赖，使测试不依赖数据库与 Redis。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    private static final String ADMIN_PHONE = "13800000001";
    private static final String USER_PHONE = "13800000002";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private ConcertShowService concertShowService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        // 限流切面通过 Redis 计数，计数为 null 时会被判定为超限而拒绝请求，
        // 这里让计数返回 1（未超限）以便验证授权之后的流程
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    /**
     * 构造带角色的认证信息，principal 为真实的 LoginUser
     */
    private static Authentication authenticationOf(String roleCode) {
        LoginUser loginUser = new LoginUser(1L,
                "ADMIN".equals(roleCode) ? ADMIN_PHONE : USER_PHONE,
                "encoded-password",
                UserStatus.NORMAL,
                Collections.emptyList(),
                Collections.singletonList(roleCode));
        return new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
    }

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        return builder.with(authentication(authenticationOf("ADMIN")));
    }

    private static MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        return builder.with(authentication(authenticationOf("USER")));
    }

    @Nested
    @DisplayName("匿名访问")
    class Anonymous {

        @Test
        @DisplayName("取票码核销需要登录")
        void testVerifyTicket_Anonymous_Unauthorized() throws Exception {
            mockMvc.perform(post("/api/ticket/verify").param("pickupCode", "TESTCODE"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("管理端接口需要登录")
        void testAdminApi_Anonymous_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/admin/order/list"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("用户端订单接口需要登录")
        void testUserOrderApi_Anonymous_Unauthorized() throws Exception {
            mockMvc.perform(get("/api/order/my"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("演唱会浏览接口无需登录")
        void testConcertBrowse_Anonymous_Allowed() throws Exception {
            mockMvc.perform(get("/api/concert/upcoming").param("page", "1").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("普通用户访问")
    class NormalUser {

        @Test
        @DisplayName("核销取票码被拒绝（曾经可核销他人取票码）")
        void testVerifyTicket_NormalUser_Forbidden() throws Exception {
            mockMvc.perform(asUser(post("/api/ticket/verify").param("pickupCode", "TESTCODE")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }

        @Test
        @DisplayName("管理端接口被拒绝")
        void testAdminApi_NormalUser_Forbidden() throws Exception {
            mockMvc.perform(asUser(get("/api/admin/order/list")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    @Nested
    @DisplayName("管理员访问")
    class Admin {

        @Test
        @DisplayName("核销取票码放行至业务逻辑")
        void testVerifyTicket_Admin_Allowed() throws Exception {
            when(orderService.verifyTicketCode("TESTCODE")).thenReturn(true);

            mockMvc.perform(asAdmin(post("/api/ticket/verify").param("pickupCode", "TESTCODE")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("管理端接口放行至业务逻辑")
        void testAdminApi_Admin_Allowed() throws Exception {
            mockMvc.perform(asAdmin(get("/api/admin/order/list").param("page", "1").param("size", "10")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }
}
