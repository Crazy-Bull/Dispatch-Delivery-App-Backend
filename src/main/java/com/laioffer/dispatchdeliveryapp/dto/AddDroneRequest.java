package com.laioffer.dispatchdeliveryapp.dto;

public record AddDroneRequest(
        String droneCode,
        Long stationId,
        Integer batteryLevel,
        double longitude,
        double latitude,
        Double altitude,
        Double speed,
        Integer status
) {}
