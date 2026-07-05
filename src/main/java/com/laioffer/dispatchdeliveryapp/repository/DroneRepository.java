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

    @Query("""
            SELECT ST_Distance(position, ST_GeogFromText(:targetWkt))
            FROM drones
            WHERE id = :id
            """)
    Optional<Double> findDistanceToTarget(@Param("id") Long id, @Param("targetWkt") String targetWkt);

    @Query("""
            SELECT ST_AsText(position::geometry)
            FROM drones
            WHERE id = :id
            """)
    Optional<String> findPositionWktById(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE drones SET
              position = CASE
                WHEN ST_Distance(position, ST_GeogFromText(:targetWkt)) <= :stepMeters
                THEN ST_GeogFromText(:targetWkt)
                ELSE ST_Project(
                  position::geography,
                  LEAST(:stepMeters, ST_Distance(position, ST_GeogFromText(:targetWkt))),
                  ST_Azimuth(
                    position::geometry,
                    ST_GeomFromText(:targetPointWkt, 4326)
                  )
                )::geography
              END,
              battery_level = GREATEST(0, LEAST(100, battery_level + :batteryDelta)),
              status = :status
            WHERE id = :id
            """)
    void updateDroneMovement(
            @Param("id") Long id,
            @Param("targetWkt") String targetWkt,
            @Param("targetPointWkt") String targetPointWkt,
            @Param("stepMeters") double stepMeters,
            @Param("batteryDelta") int batteryDelta,
            @Param("status") int status);

    @Modifying
    @Query("""
            UPDATE drones SET
              battery_level = LEAST(100, battery_level + 1)
            WHERE id = :id
            """)
    void chargeBattery(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE drones SET status = :status, speed = :speed
            WHERE id = :id
            """)
    void assignToDelivery(
            @Param("id") Long id,
            @Param("status") int status,
            @Param("speed") double speed);
}
