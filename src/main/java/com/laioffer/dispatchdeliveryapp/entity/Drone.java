package com.laioffer.dispatchdeliveryapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("drones")
public record Drone(
        @Id Long id,
        @Column("drone_code") String droneCode,
        @Column("station_id") Long stationId,
        @Column("battery_level") Integer batteryLevel,
        String position,
        Double altitude,
        Double speed,
        Integer status
) {}
