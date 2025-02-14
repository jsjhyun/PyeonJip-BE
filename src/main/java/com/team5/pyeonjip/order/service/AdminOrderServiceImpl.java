package com.team5.pyeonjip.order.service;

import com.team5.pyeonjip.global.exception.ErrorCode;
import com.team5.pyeonjip.global.exception.GlobalException;
import com.team5.pyeonjip.order.dto.AdminOrderResponseDto;
import com.team5.pyeonjip.order.entity.Order;
import com.team5.pyeonjip.order.enums.DeliveryStatus;
import com.team5.pyeonjip.order.mapper.OrderMapper;
import com.team5.pyeonjip.order.repository.OrderRepository;
import com.team5.pyeonjip.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderServiceImpl orderService;

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ORDER_NOT_FOUND));
    }

    // 주문 수정 - 배송 상태 변경
    @Transactional
    @Override
    public void updateDeliveryStatus(Long orderId, DeliveryStatus deliveryStatus) {
        Order order = findOrderById(orderId);
        order.getDelivery().updateStatus(deliveryStatus);
        orderRepository.save(order);
    }

    // 주문 삭제
    @Transactional
    @Override
    public void deleteOrderById(Long orderId) {
        Order order = findOrderById(orderId);
        orderRepository.delete(order);
    }

    //  주문 전체 조회
    @Transactional(readOnly = true)
    @Override
    public Page<AdminOrderResponseDto> findAllOrders(int page, int size, String sortField, String sortDir, String keyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return (keyword == null || keyword.isEmpty())
                ? findAllOrdersPaged(pageable)
                : findOrdersByKeyword(keyword, pageable);
    }

    private Page<AdminOrderResponseDto> findAllOrdersPaged(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::convertToAdminOrderResponseDto);
    }

    private Page<AdminOrderResponseDto> findOrdersByKeyword(String keyword, Pageable pageable) {
        return orderRepository.findOrdersByUserEmail(keyword, pageable).map(this::convertToAdminOrderResponseDto);
    }

    private AdminOrderResponseDto convertToAdminOrderResponseDto(Order order) {
        User user = order.getUser();
        double discountRate = orderService.calculateDiscountRate(user);
        Long deliveryPrice = orderService.calculateDeliveryPrice(user);
        return OrderMapper.toAdminOrderResponseDto(order, deliveryPrice, discountRate);
    }
}