package com.team5.pyeonjip.order.service;

import com.team5.pyeonjip.global.exception.ErrorCode;
import com.team5.pyeonjip.global.exception.GlobalException;
import com.team5.pyeonjip.global.exception.ResourceNotFoundException;
import com.team5.pyeonjip.order.dto.*;
import com.team5.pyeonjip.order.entity.Delivery;
import com.team5.pyeonjip.order.entity.Order;
import com.team5.pyeonjip.order.entity.OrderDetail;
import com.team5.pyeonjip.order.enums.DeliveryStatus;
import com.team5.pyeonjip.order.enums.OrderStatus;
import com.team5.pyeonjip.order.mapper.OrderMapper;
import com.team5.pyeonjip.order.repository.DeliveryRepository;
import com.team5.pyeonjip.order.repository.OrderDetailRepository;
import com.team5.pyeonjip.order.repository.OrderRepository;
import com.team5.pyeonjip.product.entity.ProductDetail;
import com.team5.pyeonjip.product.repository.ProductDetailRepository;
import com.team5.pyeonjip.product.service.ProductDetailService;
import com.team5.pyeonjip.user.entity.Grade;
import com.team5.pyeonjip.user.entity.User;
import com.team5.pyeonjip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductDetailRepository productDetailRepository;
    private final ProductDetailService productDetailService;

    private ProductDetail findProductDetailById(Long productId) { // 주문을 동시에 하게 되면, 재고 이상으로 주문이 될 수 있다.
        return productDetailRepository.findById(productId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PRODUCT_DETAIL_NOT_FOUND));
    }

    @Transactional
    @Override
    public void createOrder(CombinedOrderDto combinedOrderDto, String userEmail) {
        User user = getUserByEmail(userEmail);  // 유저 조회

        // 배송 정보 생성
        Delivery delivery = createDelivery(combinedOrderDto);

        // 주문 생성
        Long totalPrice = calculateTotalPrice(user, combinedOrderDto.getOrderCartRequestDto().getCartTotalPrice());
        Order order = createOrderEntity(combinedOrderDto, delivery, user, totalPrice);

        // 주문 상세 정보 생성 및 재고 감소 처리
        combinedOrderDto.getOrderRequestDto().getOrderDetails().forEach(orderDetailDto -> {
            reduceStock(orderDetailDto);
            createOrderDetail(order, orderDetailDto);
        });

        // 사용자 회원 등급 업데이트
        updateUserGrade(user);
    }

    // 유저 조회
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("유저를 찾을 수 없습니다."));
    }

    // 배송 정보 생성
    private Delivery createDelivery(CombinedOrderDto combinedOrderDto) {
        Delivery delivery = Delivery.builder()
                .address(combinedOrderDto.getOrderRequestDto().getAddress())
                .status(DeliveryStatus.READY)
                .build();
        return deliveryRepository.save(delivery);
    }

    // 주문 엔티티 생성
    private Order createOrderEntity(CombinedOrderDto combinedOrderDto, Delivery delivery, User user, Long totalPrice) {
        Order order = OrderMapper.toOrderEntity(combinedOrderDto.getOrderRequestDto(), delivery, user, totalPrice);
        return orderRepository.save(order);
    }

    // 재고 감소
    // TODO : Redis 분산 락
    private void reduceStock(OrderDetailDto orderDetailDto) {
        ProductDetail productDetail = findProductDetailById(orderDetailDto.getProductDetailId());
        if (productDetail.getQuantity() < orderDetailDto.getQuantity()) {
            throw new GlobalException(ErrorCode.OUT_OF_STOCK);
        }
        productDetail.setQuantity(productDetail.getQuantity() - orderDetailDto.getQuantity());
        productDetailRepository.save(productDetail);
    }

    // 주문 상세 생성
    private void createOrderDetail(Order order, OrderDetailDto orderDetailDto) {
        ProductDetail productDetail = findProductDetailById(orderDetailDto.getProductDetailId());
        OrderDetail orderDetail = OrderMapper.toOrderDetailEntity(order, productDetail, orderDetailDto);
        orderDetailRepository.save(orderDetail);
    }

    // 회원 등급 업데이트
    private void updateUserGrade(User user) {
        Long totalSpent = orderRepository.getTotalPriceByUser(user.getEmail());
        updateUserGrade(user, totalSpent);
        userRepository.save(user);
    }

    // 회원 등급에 따른 배송비 계산
    public Long calculateDeliveryPrice(User user) {
        // 기본 배송비 3000원
        long deliveryPrice = 3000L;

        if (user.getGrade().equals(Grade.GOLD)) {
            deliveryPrice = 0L;
        }
        return deliveryPrice;
    }

    // 회원 등급에 따른 할인율 계산
    public double calculateDiscountRate(User user) {
        return switch (user.getGrade()) {
            case GOLD -> 0.1; // 10% 할인
            case SILVER -> 0.05; // 5% 할인
            case BRONZE -> 0.0; // 할인 없음
        };
    }

    // 사용자 등급 업데이트 로직
    private void updateUserGrade(User user, Long totalSpent) {
        if (totalSpent >= 2000000) {
            user.setGrade(Grade.GOLD);
        } else if (totalSpent >= 1000000) {
            user.setGrade(Grade.SILVER);
        } else {
            user.setGrade(Grade.BRONZE);
        }
    }

    // 총 금액 계산
    public Long calculateTotalPrice(User user, Long cartTotalPrice) {
        // 1. 회원 등급에 따른 할인율 계산
        double discountRate = calculateDiscountRate(user);

        // 2. 회원 등급에 따른 배송비 계산
        Long deliveryPrice = calculateDeliveryPrice(user);

        // 3. 최종 금액 계산
        return Math.round(cartTotalPrice * (1 - discountRate)) + deliveryPrice;
    }

    // 주문 조회
    @Transactional(readOnly = true)
    @Override
    public List<OrderResponseDto> findOrdersByUserId(Long userId) {

        List<Order> orders = orderRepository.findOrdersByUserId(userId);

        if (orders.isEmpty()) {
            throw new GlobalException(ErrorCode.USER_ORDER_NOT_FOUND);
        }

        return orders.stream()
                .map(OrderMapper::toOrderResponseDto)
                .toList();
    }

    // 주문 취소
    @Transactional
    @Override
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ORDER_NOT_FOUND));

        // 배송 상태가 READY인 경우에만 취소 가능
        if (order.getDelivery().getStatus() != DeliveryStatus.READY) {
            throw new GlobalException(ErrorCode.DELIVERY_ALREADY_STARTED);
        }

        // 주문 상태 업데이트
        order.updateStatus(OrderStatus.CANCEL);

        // 재고 복구
        restoreProductStock(order);
    }

    // 재고 복구
    // TODO : Redis 분산 락
    private void restoreProductStock(Order order) {
        order.getOrderDetails().forEach(orderDetail -> {
            ProductDetail productDetail = findProductDetailById(orderDetail.getProduct().getId());
            productDetailService.updateDetailQuantity(productDetail.getId(), orderDetail.getQuantity());
        });
    }

    // 장바구니 데이터 가공
    @Override
    public OrderCartResponseDto getOrderSummary(OrderCartRequestDto orderCartRequestDto) {

        String userEmail = orderCartRequestDto.getEmail();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Long cartTotalPrice = orderCartRequestDto
                .getCartTotalPrice();

        double discountRate = calculateDiscountRate(user);
        Long deliveryPrice = calculateDeliveryPrice(user);
        Long totalPrice = Math.round(cartTotalPrice * (1 - discountRate)) + deliveryPrice;

        List<OrderDetailDto> orderDetails = orderCartRequestDto.getOrderDetails();

        return new OrderCartResponseDto(cartTotalPrice, totalPrice, deliveryPrice, discountRate, orderDetails);
    }
}