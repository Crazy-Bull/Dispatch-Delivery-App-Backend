package com.laioffer.dispatchdeliveryapp.dto;

public record OrderTrackingResponse(
        Long orderId,
        Integer orderStatus,
        boolean trackable,
        GeoPoint dronePosition,
        GeoPoint deliveryDestination,
        GeoPoint stationPosition,
        String droneCode,
        Integer droneStatus,
        Double droneSpeed,
        Integer droneBattery
) {}
