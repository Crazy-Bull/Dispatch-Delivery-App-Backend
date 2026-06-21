package com.laioffer.dispatchdeliveryapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("orders")
public record Order(
        @Id Long id,
        @Column("order_no") String orderNo,
        @Column("user_id") Long userId,
        @Column("station_id") Long stationId,
        @Column("assigned_drone_id") Long assignedDroneId,
        @Column("delivery_position") String deliveryPosition,
        Integer status,
        @Column("created_at") LocalDateTime createdAt,
        @Column("assigned_at") LocalDateTime assignedAt,
        @Column("delivered_at") LocalDateTime deliveredAt,
        @Column("completed_at") LocalDateTime completedAt
) {}
