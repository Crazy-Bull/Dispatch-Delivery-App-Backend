package com.laioffer.dispatchdeliveryapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("products")
public record Product(
        @Id Long id,
        String name,
        String description,
        BigDecimal price,
        @Column("image_url") String imageUrl
) {}
