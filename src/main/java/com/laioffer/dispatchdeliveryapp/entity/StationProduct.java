package com.laioffer.dispatchdeliveryapp.entity;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("station_products")
public record StationProduct(
        @Column("station_id") Long stationId,
        @Column("product_id") Long productId,
        Integer stock
) {}
