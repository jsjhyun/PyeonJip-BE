package com.team5.pyeonjip.cart.controller;

import com.team5.pyeonjip.cart.dto.CartDetailDto;
import com.team5.pyeonjip.cart.dto.CartDto;
import com.team5.pyeonjip.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    private final CartService cartService;

    // 장바구니 페이지
        @GetMapping
    public ResponseEntity<Void> cart(){
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 로컬 -> 서버
    @PostMapping("/syncLocal")
    public ResponseEntity<List<CartDto>> syncCart(@RequestBody List<CartDto> localCartItems, @RequestParam Long userId) {
        List<CartDto> dtos = cartService.syncCart(userId, localCartItems);
        return ResponseEntity.status(HttpStatus.OK).body(dtos);
    }

    // 서버 -> 로컬
    @PostMapping("/syncServer")
    public ResponseEntity<List<CartDto>> syncCart(@RequestParam Long userId) {
        List<CartDto> dtos = cartService.getCartItemsByUserId(userId);
        return ResponseEntity.status(HttpStatus.OK).body(dtos);
    }

    // DetailDto 가져오기
    @PostMapping("/detail")
    public ResponseEntity<List<CartDetailDto>> getCartDetail(@RequestBody List<CartDto> cartDtos) {
        List<CartDetailDto> detailDtos = cartService.getCartDetailsByCartDto(cartDtos);

        return ResponseEntity.status(HttpStatus.OK).body(detailDtos);
    }

    @PostMapping("/add")
    public ResponseEntity<CartDto> addCart(@RequestBody CartDto cartDto, @RequestParam Long userId) {
           CartDto dto = cartService.addCartDto(cartDto,userId);
           return ResponseEntity.status(HttpStatus.OK).body(dto);
    }


    // 테스트 샌드박스용 페이지
    @GetMapping("/sandbox")
    public List<CartDto> sandbox() {
        List<CartDto>  target = new ArrayList<>();
        List<CartDetailDto> target1 = new ArrayList<>();
        CartDto dto1 = cartService.getCartDto(1L);
        CartDto dto2 = cartService.getCartDto(2L);
        CartDto dto3 = cartService.getCartDto(3L);
        CartDto dto4 = cartService.getCartDto(4L);
        CartDto dto5 = cartService.getCartDto(5L);
        CartDto dto6 = cartService.getCartDto(6L);
        CartDto dto7 = cartService.getCartDto(7L);
        CartDto dto8 = cartService.getCartDto(8L);
        target.add(dto1);
        target.add(dto2);
        target.add(dto3);
        target.add(dto4);
        target.add(dto5);
        target.add(dto6);
        target.add(dto7);
        target.add(dto8);

        return target;
    }
}

