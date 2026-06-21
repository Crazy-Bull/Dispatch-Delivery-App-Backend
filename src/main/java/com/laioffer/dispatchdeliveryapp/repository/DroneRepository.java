package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.Drone;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DroneRepository extends ListCrudRepository<Drone, Long> {

    Optional<Drone> findByDroneCode(String droneCode);

    @Query("""
            SELECT * FROM drones
            WHERE station_id = :stationId
              AND status = :status
              AND battery_level >= :minBatteryLevel
            """)
    List<Drone> findByStationIdAndStatusAndMinBatteryLevel(
            @Param("stationId") Long stationId,
            @Param("status") Integer status,
            @Param("minBatteryLevel") Integer minBatteryLevel);

    @Modifying
    @Query("""
            INSERT INTO drones (drone_code, station_id, battery_level, position, altitude, speed, status)
            VALUES (:droneCode, :stationId, :batteryLevel, ST_GeogFromText(:positionWkt), :altitude, :speed, :status)
            """)
    void insertDrone(
            @Param("droneCode") String droneCode,
            @Param("stationId") Long stationId,
            @Param("batteryLevel") Integer batteryLevel,
            @Param("positionWkt") String positionWkt,
            @Param("altitude") Double altitude,
            @Param("speed") Double speed,
            @Param("status") Integer status);
}
