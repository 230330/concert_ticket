package com.concert.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.concert.config.AlipayProperties;
import com.concert.entity.Order;
import com.concert.enums.OrderStatus;
import com.concert.service.OrderService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付宝回调处理单元测试
 *
 * <p>回调地址 /api/pay/alipay/notify 是免登录接口，却能改订单状态，因此这里重点锁住：
 * 1) 验签失败（或参数畸形）时必须返回 failure 且绝不更新订单；
 * 2) 验签通过、交易成功、订单待支付时才置为已支付；
 * 3) 重复通知不会重复处理（幂等由订单状态保证）；
 * 4) 非成功交易状态只确认收到，不动订单。
 *
 * <p>测试用真实 RSA 密钥对自签回调参数，走完整验签链路，而不是 mock 掉 AlipaySignature。
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static String privateKey;
    private static String publicKey;
    private static String otherPrivateKey;

    @Mock
    private OrderService orderService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private PaymentServiceImpl paymentService;

    @BeforeAll
    static void prepareKeyPairAndTableInfo() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // 另一对密钥：签名格式合法但验不过，用于模拟伪造者用自己的密钥签名
        KeyPair otherKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        otherPrivateKey = Base64.getEncoder().encodeToString(otherKeyPair.getPrivate().getEncoded());

        // LambdaQueryWrapper 的方法引用依赖 MyBatis-Plus 实体元数据缓存，纯 Mockito 单测需手动初始化
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Order.class);
    }

    @BeforeEach
    void setUp() {
        AlipayProperties alipayProperties = new AlipayProperties();
        alipayProperties.setPublicKey(publicKey);
        alipayProperties.setCharset("UTF-8");
        alipayProperties.setSignType("RSA2");

        paymentService = new PaymentServiceImpl();
        ReflectionTestUtils.setField(paymentService, "orderService", orderService);
        ReflectionTestUtils.setField(paymentService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(paymentService, "alipayProperties", alipayProperties);
        // messageProducer 保持为 null：实现里用 != null 判断，等价于未启用 MQ
    }

    /**
     * 构造一条带有效签名的回调参数
     */
    private static Map<String, String> signedNotifyParams(String orderNo, String tradeStatus) throws AlipayApiException {
        return signedNotifyParams(orderNo, tradeStatus, privateKey);
    }

    /**
     * 用指定私钥签名，便于构造"签名合法但验不过"的伪造回调
     */
    private static Map<String, String> signedNotifyParams(String orderNo, String tradeStatus,
                                                          String signingKey) throws AlipayApiException {
        Map<String, String> params = new TreeMap<>();
        params.put("app_id", "2021000000000000");
        params.put("out_trade_no", orderNo);
        params.put("trade_no", "2026041722001400000000000001");
        params.put("trade_status", tradeStatus);
        params.put("total_amount", "299.00");

        // 签名内容为去掉 sign 与 sign_type 后的有序参数串
        String content = AlipaySignature.getSignContent(new TreeMap<>(params));
        params.put("sign_type", "RSA2");
        params.put("sign", AlipaySignature.rsaSign(content, signingKey, "UTF-8", "RSA2"));
        return params;
    }

    private static Order pendingOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORDER123");
        order.setUserId(100L);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    @Nested
    @DisplayName("验签不通过")
    class SignatureRejected {

        @Test
        @DisplayName("签名被篡改时返回 failure 且不查询、不更新订单")
        void testNotify_TamperedSign_ReturnsFailure() throws Exception {
            Map<String, String> params = signedNotifyParams("ORDER123", "TRADE_SUCCESS");
            params.put("sign", "tampered-signature-value");

            assertEquals("failure", paymentService.handleAlipayNotify(params));

            verify(orderService, never()).getOne(any());
            verify(orderService, never()).updateById(any());
        }

        @Test
        @DisplayName("用他人密钥伪造的合法签名被拒绝，订单不被更新")
        void testNotify_ForgedSignatureWithOtherKey_ReturnsFailure() throws Exception {
            // 订单真实存在，一旦验签被绕过就会走到更新逻辑，断言才有意义；
            // 验签正常时该桩不会被使用，属预期，故用 lenient
            lenient().when(orderService.getOne(any())).thenReturn(pendingOrder());

            Map<String, String> params = signedNotifyParams("ORDER123", "TRADE_SUCCESS", otherPrivateKey);

            assertEquals("failure", paymentService.handleAlipayNotify(params));

            verify(orderService, never()).updateById(any());
        }

        @Test
        @DisplayName("参数为空（伪造的回调）返回 failure 且不动订单")
        void testNotify_EmptyParams_ReturnsFailure() {
            assertEquals("failure", paymentService.handleAlipayNotify(Collections.emptyMap()));

            verify(orderService, never()).updateById(any());
        }

        @Test
        @DisplayName("金额被改动导致验签失败时订单不会被置为已支付")
        void testNotify_AmountTampered_ReturnsFailure() throws Exception {
            // 同上：订单真实存在，仅用于让"验签被绕过"这种情况能被断言捕获
            lenient().when(orderService.getOne(any())).thenReturn(pendingOrder());

            Map<String, String> params = signedNotifyParams("ORDER123", "TRADE_SUCCESS");
            params.put("total_amount", "0.01"); // 改了金额但签名不变

            assertEquals("failure", paymentService.handleAlipayNotify(params));

            verify(orderService, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("验签通过")
    class SignaturePassed {

        @Test
        @DisplayName("待支付订单被置为已支付并记录支付时间")
        void testNotify_PendingOrder_MarksPaid() throws Exception {
            when(orderService.getOne(any())).thenReturn(pendingOrder());

            String result = paymentService.handleAlipayNotify(signedNotifyParams("ORDER123", "TRADE_SUCCESS"));

            assertEquals("success", result);
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderService).updateById(captor.capture());
            assertEquals(OrderStatus.PAID, captor.getValue().getStatus());
            assertNotNull(captor.getValue().getPayTime());
        }

        @Test
        @DisplayName("TRADE_FINISHED 同样按支付成功处理")
        void testNotify_TradeFinished_MarksPaid() throws Exception {
            when(orderService.getOne(any())).thenReturn(pendingOrder());

            assertEquals("success",
                    paymentService.handleAlipayNotify(signedNotifyParams("ORDER123", "TRADE_FINISHED")));

            verify(orderService).updateById(any());
        }

        @Test
        @DisplayName("重复通知不会二次更新订单（幂等）")
        void testNotify_AlreadyPaid_NoSecondUpdate() throws Exception {
            Order paidOrder = pendingOrder();
            paidOrder.setStatus(OrderStatus.PAID);
            when(orderService.getOne(any())).thenReturn(paidOrder);

            assertEquals("success",
                    paymentService.handleAlipayNotify(signedNotifyParams("ORDER123", "TRADE_SUCCESS")));

            verify(orderService, never()).updateById(any());
        }

        @Test
        @DisplayName("订单不存在时返回 failure 且不更新任何订单")
        void testNotify_OrderNotFound_ReturnsFailure() throws Exception {
            when(orderService.getOne(any())).thenReturn(null);

            assertEquals("failure",
                    paymentService.handleAlipayNotify(signedNotifyParams("NOT_EXIST_ORDER", "TRADE_SUCCESS")));

            verify(orderService, never()).updateById(any());
        }

        @Test
        @DisplayName("非成功交易状态只确认收到，不触碰订单")
        void testNotify_WaitBuyerPay_NoOrderUpdate() throws Exception {
            assertEquals("success",
                    paymentService.handleAlipayNotify(signedNotifyParams("ORDER123", "WAIT_BUYER_PAY")));

            verify(orderService, never()).getOne(any());
            verify(orderService, never()).updateById(any());
        }
    }
}
