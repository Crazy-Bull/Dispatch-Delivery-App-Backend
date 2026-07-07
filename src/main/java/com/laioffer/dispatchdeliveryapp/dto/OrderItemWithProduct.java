package com.laioffer.dispatchdeliveryapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;

// Spring Data JDBC maps record components to columns using its naming
// strategy (snake_case by default), so we bind each component to the
// actual SQL column explicitly. Jackson then renames to camelCase on the
// way out so the frontend keeps reading productName / productImageUrl.
public record OrderItemWithProduct(
        @Column("product_id") @JsonProperty("productId") Long productId,
        @Column("name") @JsonProperty("productName") String productName,
        @Column("image_url") @JsonProperty("productImageUrl") String productImageUrl,
        @Column("quantity") Integer quantity,
        @Column("unit_price") @JsonProperty("unitPrice") BigDecimal unitPrice
) {}