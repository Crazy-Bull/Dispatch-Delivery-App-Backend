package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.config.MockDeliveryProperties;
import com.laioffer.dispatchdeliveryapp.entity.Drone;
import com.laioffer.dispatchdeliveryapp.model.DroneStatus;
import com.laioffer.dispatchdeliveryapp.repository.AbstractPostgresRepositoryTest;
import com.laioffer.dispatchdeliveryapp.repository.DroneRepository;
import com.laioffer.dispatchdeliveryapp.repository.RepositoryTestConfig;
import com.laioffer.dispatchdeliveryapp.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Import({RepositoryTestConfig.class, MockDeliveryTestConfig.class, MockDeliveryService.class})
class MockDeliveryServiceTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private MockDeliveryService mockDeliveryService;

    @Autowired
    private DroneRepository droneRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void tick_movesDeliveryDroneTowardOrderAndDecreasesBattery() {
        Drone drone = droneRepository.findByDroneCode("DRONE-M1").orElseThrow();
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", Long.class);

        jdbcTemplate.update("""
                UPDATE drones
                SET speed = 10, status = ?, battery_level = 80
                WHERE id = ?
                """, DroneStatus.DELIVERY, drone.id());

        jdbcTemplate.update("""
                INSERT INTO orders (order_no, user_id, station_id, assigned_drone_id, delivery_position, status, total_amount, assigned_at)
                VALUES (?, ?, ?, ?, ST_GeogFromText(?), 1, 0, CURRENT_TIMESTAMP)
                """,
                "ORD-TEST-1",
                userId,
                drone.stationId(),
                drone.id(),
                pointWkt(-122.4100, 37.7800));

        double distanceBefore = droneRepository.findDistanceToTarget(
                drone.id(),
                "SRID=4326;POINT(-122.4100 37.7800)").orElseThrow();

        mockDeliveryService.tick();

        Drone updated = droneRepository.findById(drone.id()).orElseThrow();
        double distanceAfter = droneRepository.findDistanceToTarget(
                drone.id(),
                "SRID=4326;POINT(-122.4100 37.7800)").orElseThrow();

        assertThat(distanceAfter).isLessThan(distanceBefore);
        assertThat(updated.batteryLevel()).isEqualTo(79);
        assertThat(updated.status()).isEqualTo(DroneStatus.DELIVERY);
    }

    @Test
    @Transactional
    void tick_switchesToReturnWhenDeliveryDestinationReached() {
        Drone drone = droneRepository.findByDroneCode("DRONE-M2").orElseThrow();
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", Long.class);
        String deliveryWkt = pointWkt(-122.4194, 37.7749);

        jdbcTemplate.update("""
                UPDATE drones
                SET speed = 100, status = ?, battery_level = 50, position = ST_GeogFromText(?)
                WHERE id = ?
                """, DroneStatus.DELIVERY, deliveryWkt, drone.id());

        jdbcTemplate.update("""
                INSERT INTO orders (order_no, user_id, station_id, assigned_drone_id, delivery_position, status, total_amount, assigned_at)
                VALUES (?, ?, ?, ?, ST_GeogFromText(?), 1, 0, CURRENT_TIMESTAMP)
                """,
                "ORD-TEST-2",
                userId,
                drone.stationId(),
                drone.id(),
                deliveryWkt);

        mockDeliveryService.tick();

        Drone updated = droneRepository.findById(drone.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(DroneStatus.RETURN);
        assertThat(updated.batteryLevel()).isEqualTo(49);
    }

    @Test
    @Transactional
    void tick_movesReturnDroneTowardStationAndDecreasesBattery() {
        Drone drone = droneRepository.findByDroneCode("DRONE-A1").orElseThrow();
        String stationWkt = stationRepository.findPositionWktById(drone.stationId()).orElseThrow();

        jdbcTemplate.update("""
                UPDATE drones
                SET speed = 10, status = ?, battery_level = 60, position = ST_GeogFromText(?)
                WHERE id = ?
                """, DroneStatus.RETURN, pointWkt(-122.4300, 37.8000), drone.id());

        double distanceBefore = droneRepository.findDistanceToTarget(
                drone.id(),
                "SRID=4326;" + stationWkt).orElseThrow();

        mockDeliveryService.tick();

        Drone updated = droneRepository.findById(drone.id()).orElseThrow();
        double distanceAfter = droneRepository.findDistanceToTarget(
                drone.id(),
                "SRID=4326;" + stationWkt).orElseThrow();

        assertThat(distanceAfter).isLessThan(distanceBefore);
        assertThat(updated.batteryLevel()).isEqualTo(59);
        assertThat(updated.status()).isEqualTo(DroneStatus.RETURN);
    }

    @Test
    @Transactional
    void tick_chargesWaitingDrone() {
        Drone drone = droneRepository.findByDroneCode("DRONE-S1").orElseThrow();

        jdbcTemplate.update("""
                UPDATE drones
                SET status = ?, battery_level = 80
                WHERE id = ?
                """, DroneStatus.WAITING, drone.id());

        mockDeliveryService.tick();

        Drone updated = droneRepository.findById(drone.id()).orElseThrow();
        assertThat(updated.batteryLevel()).isEqualTo(81);
        assertThat(updated.status()).isEqualTo(DroneStatus.WAITING);
    }

    @Test
    @Transactional
    void tick_doesNotChargeWaitingDroneAboveMaxBattery() {
        Drone drone = droneRepository.findByDroneCode("DRONE-S2").orElseThrow();

        jdbcTemplate.update("""
                UPDATE drones
                SET status = ?, battery_level = 100
                WHERE id = ?
                """, DroneStatus.WAITING, drone.id());

        mockDeliveryService.tick();

        Drone updated = droneRepository.findById(drone.id()).orElseThrow();
        assertThat(updated.batteryLevel()).isEqualTo(100);
    }

    @Test
    @Transactional
    void tick_switchesToWaitingWhenStationReached() {
        Drone drone = droneRepository.findByDroneCode("DRONE-A2").orElseThrow();
        String stationWkt = stationRepository.findPositionWktById(drone.stationId()).orElseThrow();

        jdbcTemplate.update("""
                UPDATE drones
                SET speed = 100, status = ?, battery_level = 90, position = ST_GeogFromText(?)
                WHERE id = ?
                """, DroneStatus.RETURN, "SRID=4326;" + stationWkt, drone.id());

        mockDeliveryService.tick();

        Drone updated = droneRepository.findById(drone.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(DroneStatus.WAITING);
        assertThat(updated.batteryLevel()).isEqualTo(89);
    }
}
