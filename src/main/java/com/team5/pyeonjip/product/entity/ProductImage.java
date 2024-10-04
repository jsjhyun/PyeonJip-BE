package com.team5.pyeonjip.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl; // 이미지 URL

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

}