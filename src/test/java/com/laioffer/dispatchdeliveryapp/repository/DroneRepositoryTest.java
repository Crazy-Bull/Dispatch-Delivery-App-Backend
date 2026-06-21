package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.Drone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RepositoryTestConfig.class)
class DroneRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private DroneRepository droneRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findAll_returnsSeededDrones() {
        List<Drone> drones = droneRepository.findAll();

        assertThat(drones).hasSize(6);
        assertThat(drones).extracting(Drone::droneCode)
                .containsExactlyInAnyOrder(
                        "DRONE-M1", "DRONE-M2",
                        "DRONE-A1", "DRONE-A2",
                        "DRONE-S1", "DRONE-S2");
    }

    @Test
    void findById_returnsMatchingDrone() {
        Drone drone = droneRepository.findByDroneCode("DRONE-M1").orElseThrow();

        Drone found = droneRepository.findById(drone.id()).orElseThrow();

        assertThat(found.droneCode()).isEqualTo("DRONE-M1");
        assertThat(found.batteryLevel()).isEqualTo(100);
        assertThat(found.status()).isZero();
        assertThat(found.position()).isNotBlank();
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        assertThat(droneRepository.findById(999_999L)).isEmpty();
    }

    @Test
    void findByDroneCode_returnsMatchingDrone() {
        Drone drone = droneRepository.findByDroneCode("DRONE-A2").orElseThrow();

        assertThat(drone.stationId()).isEqualTo(
                jdbcTemplate.queryForObject(
                        "SELECT id FROM stations WHERE name = 'Marina Hub'",
                        Long.class));
    }

    @Test
    void findByStationIdAndStatusAndMinBatteryLevel_returnsMatchingDrones() {
        Long missionHubId = missionHubStationId(jdbcTemplate);

        List<Drone> drones = droneRepository.findByStationIdAndStatusAndMinBatteryLevel(missionHubId, 0, 80);

        assertThat(drones).extracting(Drone::droneCode)
                .containsExactlyInAnyOrder("DRONE-M1", "DRONE-M2");
    }

    @Test
    void findByStationIdAndStatusAndMinBatteryLevel_excludesLowBatteryDrones() {
        Long missionHubId = missionHubStationId(jdbcTemplate);

        jdbcTemplate.update(
                "UPDATE drones SET battery_level = 50 WHERE drone_code = 'DRONE-M1'");

        List<Drone> drones = droneRepository.findByStationIdAndStatusAndMinBatteryLevel(missionHubId, 0, 80);

        assertThat(drones).extracting(Drone::droneCode).containsExactly("DRONE-M2");
    }

    @Test
    void findByStationIdAndStatusAndMinBatteryLevel_returnsEmptyWhenNoMatch() {
        Long missionHubId = missionHubStationId(jdbcTemplate);

        List<Drone> drones = droneRepository.findByStationIdAndStatusAndMinBatteryLevel(missionHubId, 0, 101);

        assertThat(drones).isEmpty();
    }

    @Test
    @Transactional
    void insertDrone_persistsNewDrone() {
        Long missionHubId = missionHubStationId(jdbcTemplate);

        droneRepository.insertDrone(
                "DRONE-TEST-1",
                missionHubId,
                90,
                pointWkt(-122.4194, 37.7749),
                10.0,
                5.0,
                1);

        Drone drone = droneRepository.findByDroneCode("DRONE-TEST-1").orElseThrow();

        assertThat(drone.stationId()).isEqualTo(missionHubId);
        assertThat(drone.batteryLevel()).isEqualTo(90);
        assertThat(drone.altitude()).isEqualTo(10.0);
        assertThat(drone.speed()).isEqualTo(5.0);
        assertThat(drone.status()).isEqualTo(1);
        assertThat(drone.position()).isNotBlank();
    }

    @Test
    @Transactional
    void deleteById_removesDrone() {
        Drone drone = droneRepository.findByDroneCode("DRONE-S2").orElseThrow();

        droneRepository.deleteById(drone.id());

        assertThat(droneRepository.findById(drone.id())).isEmpty();
        assertThat(droneRepository.findByDroneCode("DRONE-S2")).isEmpty();
    }
}
