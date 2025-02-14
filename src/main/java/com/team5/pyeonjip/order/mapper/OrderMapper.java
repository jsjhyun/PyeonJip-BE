package com.team5.pyeonjip.order.mapper;

import com.team5.pyeonjip.order.dto.AdminOrderResponseDto;
import com.team5.pyeonjip.order.dto.OrderDetailDto;
import com.team5.pyeonjip.order.dto.OrderRequestDto;
import com.team5.pyeonjip.order.dto.OrderResponseDto;
import com.team5.pyeonjip.order.entity.Delivery;
import com.team5.pyeonjip.order.entity.Order;
import com.team5.pyeonjip.order.entity.OrderDetail;
import com.team5.pyeonjip.order.enums.OrderStatus;
import com.team5.pyeonjip.product.entity.ProductDetail;
import com.team5.pyeonjip.user.entity.User;

public class OrderMapper {

    // 사용자 : entity -> dto
    public static OrderResponseDto toOrderResponseDto(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .orderStatus(order.getStatus())
                .deliveryStatus(order.getDelivery().getStatus())
                .createdAt(order.getCreatedAt())
                .totalPrice(order.getTotalPrice())
                .orderDetails(order.getOrderDetails().stream()
                        .map(detail -> OrderDetailDto.builder()
                                .productDetailId(detail.getProduct().getId())
                                .productName(detail.getProductName())
                                .productPrice(detail.getProductPrice())
                                .subTotalPrice(detail.getSubTotalPrice())
                                .productImage(detail.getProduct().getMainImage())
                                .quantity(detail.getQuantity())
                                .build())
                        .toList())
                .build();
    }

    // 관리자 : entity -> dto
    public static AdminOrderResponseDto toAdminOrderResponseDto(Order order, Long deliveryPrice, double discountRate) { // deliveryPrice,discountRate 엔티티 필드 추가
        return AdminOrderResponseDto.builder()
                .id(order.getId())
                .userEmail(order.getUser().getEmail())
                .userName(order.getUser().getName())
                .phoneNumber(order.getUser().getPhoneNumber())
                .orderStatus(order.getStatus())
                .totalPrice(order.getTotalPrice()) // 전체 주문 금액
                .createdAt(order.getCreatedAt())
                .deliveryStatus(order.getDelivery().getStatus())
                .deliveryPrice(deliveryPrice) // 배송비 추가
                .discountRate(discountRate) // 할인율 추가
                .orderDetails(order.getOrderDetails().stream()
                        .map(detail -> OrderDetailDto.builder()
                                .productDetailId(detail.getId())
                                .productName(detail.getProductName())
                                .productDetailName(detail.getProductDetailName())
                                .subTotalPrice(detail.getSubTotalPrice()) // 상품 상세 수량 * 가격
                                .quantity(detail.getQuantity())
                                .productPrice(detail.getProductPrice()) // 개당 가격
                                .productImage(detail.getProduct().getMainImage())
                                .build())
                        .toList())
                .build();
    }

    // 주문 저장 : dto -> entity
    public static Order toOrderEntity(OrderRequestDto orderRequestDto, Delivery delivery, User user, Long totalPrice) {
        return Order.builder()
                .recipient(orderRequestDto.getRecipient() != null ? orderRequestDto.getRecipient() : user.getName()) // 수령인, 유저 기본 이름 사용
                .phoneNumber(orderRequestDto.getPhoneNumber() != null ? orderRequestDto.getPhoneNumber() : user.getPhoneNumber())   // 연락처, 유저 기본 연락처 사용
                .requirement(orderRequestDto.getRequirement()) // 요청사항은 입력받은 값 사용
                .status(OrderStatus.ORDER)                     // 주문 상태는 기본적으로 ORDER로 설정
                .delivery(delivery)
                .totalPrice(totalPrice)
                .user(user)
                .build();
    }

    // 주문 상세 저장 : dto -> entity
    public static OrderDetail toOrderDetailEntity(Order order, ProductDetail product, OrderDetailDto orderDetailDto) {
        return OrderDetail.builder()
                .product(product)
                .order(order)
                .productName(orderDetailDto.getProductName())
                .quantity(orderDetailDto.getQuantity())
                .productPrice(orderDetailDto.getProductPrice())
                .build();
    }
}