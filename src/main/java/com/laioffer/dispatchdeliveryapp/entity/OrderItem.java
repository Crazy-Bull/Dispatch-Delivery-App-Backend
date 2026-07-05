package com.laioffer.dispatchdeliveryapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("order_items")
public record OrderItem(
        @Id Long id,
        @Column("order_id") Long orderId,
        @Column("product_id") Long productId,
        Integer quantity,
        @Column("unit_price") BigDecimal unitPrice
) {}
