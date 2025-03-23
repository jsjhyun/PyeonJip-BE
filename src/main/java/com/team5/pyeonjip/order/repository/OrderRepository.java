package com.team5.pyeonjip.order.repository;

import com.team5.pyeonjip.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 주문 전체 조회
    Page<Order> findAll(Pageable pageable);

    // 사용자 이메일로 주문 조회
    @Query("SELECT o FROM Order o WHERE o.user.email LIKE %:userEmail%")
    Page<Order> findOrdersByUserEmail(@Param("userEmail") String userEmail, Pageable pageable);

    // 사용자 ID로 모든 주문 조회
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findOrdersByUserId(@Param("userId") Long userId);

    // 사용자의 모든 주문 금액 합산
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM Order o WHERE o.user.email = :userEmail")
    Long getTotalPriceByUser(@Param("userEmail") String userEmail);
}