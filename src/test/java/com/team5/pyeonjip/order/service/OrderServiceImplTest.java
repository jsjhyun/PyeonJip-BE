package com.team5.pyeonjip.order.service;

import com.team5.pyeonjip.order.dto.OrderDetailDto;
import com.team5.pyeonjip.order.repository.DeliveryRepository;
import com.team5.pyeonjip.order.repository.OrderDetailRepository;
import com.team5.pyeonjip.order.repository.OrderRepository;
import com.team5.pyeonjip.product.entity.ProductDetail;
import com.team5.pyeonjip.product.repository.ProductDetailRepository;
import com.team5.pyeonjip.product.service.ProductDetailService;
import com.team5.pyeonjip.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private OrderServiceImpl orderService;
    private ProductDetailRepository productDetailRepository;
    private RedissonClient redissonClient;
    private RLock rLock;

    private ProductDetail productDetail;

    @BeforeEach
    void setUp() {
        productDetailRepository = mock(ProductDetailRepository.class);
        redissonClient = mock(RedissonClient.class);
        rLock = mock(RLock.class);

        // 필요한 모든 Repository 및 Service를 Mocking
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DeliveryRepository deliveryRepository = mock(DeliveryRepository.class);
        OrderDetailRepository orderDetailRepository = mock(OrderDetailRepository.class);
        ProductDetailService productDetailService = mock(ProductDetailService.class);

        // 올바른 생성자 사용
        orderService = new OrderServiceImpl(
                redissonClient, orderRepository, userRepository, deliveryRepository,
                orderDetailRepository, productDetailRepository, productDetailService
        );

        // 초기 재고 100개
        productDetail = new ProductDetail();
        productDetail.setId(1L);
        productDetail.setQuantity(100L);
    }


    @Test
    void testReduceStock_Concurrency() throws InterruptedException {
        int threadCount = 10;  // 스레드 10개
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount); // 동일 실행
        CountDownLatch latch = new CountDownLatch(threadCount);

        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(productDetailRepository.findById(1L)).thenReturn(Optional.of(productDetail));

        System.out.println("초기 수량: " + productDetail.getQuantity());

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.reduceStock(new OrderDetailDto(1L, "상품", 1L, 1000L, 1000L, "이미지", "디테일"));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(90, productDetail.getQuantity());
    }
}
