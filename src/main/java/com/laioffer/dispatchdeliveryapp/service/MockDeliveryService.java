package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.config.MockDeliveryProperties;
import com.laioffer.dispatchdeliveryapp.entity.Drone;
import com.laioffer.dispatchdeliveryapp.model.DroneStatus;
import com.laioffer.dispatchdeliveryapp.repository.DroneRepository;
import com.laioffer.dispatchdeliveryapp.repository.OrderRepository;
import com.laioffer.dispatchdeliveryapp.repository.StationRepository;
import com.laioffer.dispatchdeliveryapp.util.GeographyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MockDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(MockDeliveryService.class);

    private final DroneRepository droneRepository;
    private final OrderRepository orderRepository;
    private final StationRepository stationRepository;
    private final MockDeliveryProperties properties;

    public MockDeliveryService(
            DroneRepository droneRepository,
            OrderRepository orderRepository,
            StationRepository stationRepository,
            MockDeliveryProperties properties) {
        this.droneRepository = droneRepository;
        this.orderRepository = orderRepository;
        this.stationRepository = stationRepository;
        this.properties = properties;
    }

    @Transactional
    public void tick() {
        double tickSeconds = properties.tickIntervalMs() / 1000.0;
        List<Drone> drones = droneRepository.findAll();

        // #region agent log
        try (var fw = new java.io.FileWriter("debug-591cf6.log", true)) {
            fw.write("{\"sessionId\":\"591cf6\",\"hypothesisId\":\"H3\",\"location\":\"MockDeliveryService.tick\",\"message\":\"tick start\",\"data\":{\"droneCount\":" + drones.size() + ",\"tickIntervalMs\":" + properties.tickIntervalMs() + ",\"tickSeconds\":" + tickSeconds + "},\"timestamp\":" + System.currentTimeMillis() + ",\"runId\":\"pre-fix\"}\n");
        } catch (Exception ignored) {}
        // #endregion

        for (Drone drone : drones) {
            int status = drone.status();
            if (status == DroneStatus.WAITING) {
                processWaiting(drone);
                continue;
            }

            double stepMeters = drone.speed() * tickSeconds;
            if (stepMeters <= 0) {
                continue;
            }

            if (status == DroneStatus.DELIVERY) {
                processDelivery(drone, stepMeters);
            } else if (status == DroneStatus.RETURN) {
                processReturn(drone, stepMeters);
            }
        }
    }

    private void processWaiting(Drone drone) {
        droneRepository.chargeBattery(drone.id());
    }

    private void processDelivery(Drone drone, double stepMeters) {
        String targetPointWkt = orderRepository.findDeliveryPositionWktByAssignedDroneId(drone.id())
                .orElse(null);

        // #region agent log
        try (var fw = new java.io.FileWriter("debug-591cf6.log", true)) {
            fw.write("{\"sessionId\":\"591cf6\",\"hypothesisId\":\"H2\",\"location\":\"MockDeliveryService.processDelivery\",\"message\":\"delivery target lookup\",\"data\":{\"droneId\":" + drone.id() + ",\"droneCode\":\"" + drone.droneCode() + "\",\"targetPointWkt\":" + (targetPointWkt == null ? "null" : "\"" + targetPointWkt.replace("\"", "\\\"") + "\"") + ",\"stepMeters\":" + stepMeters + "},\"timestamp\":" + System.currentTimeMillis() + ",\"runId\":\"pre-fix\"}\n");
        } catch (Exception ignored) {}
        // #endregion

        if (targetPointWkt == null) {
            log.warn("Drone {} is in delivery mode but has no assigned order", drone.droneCode());
            return;
        }

        moveDrone(drone, targetPointWkt, stepMeters, -1, DroneStatus.RETURN, DroneStatus.DELIVERY);
    }

    private void processReturn(Drone drone, double stepMeters) {
        String targetPointWkt = stationRepository.findPositionWktById(drone.stationId())
                .orElse(null);
        if (targetPointWkt == null) {
            log.warn("Drone {} has unknown station {}", drone.droneCode(), drone.stationId());
            return;
        }

        moveDrone(drone, targetPointWkt, stepMeters, -1, DroneStatus.WAITING, DroneStatus.RETURN);
    }

    private void moveDrone(
            Drone drone,
            String targetPointWkt,
            double stepMeters,
            int batteryDelta,
            int arrivedStatus,
            int inTransitStatus) {
        String targetWkt = GeographyUtils.toGeographyWkt(targetPointWkt);
        String pointWkt = GeographyUtils.toPointWkt(targetPointWkt);

        double distance = droneRepository.findDistanceToTarget(drone.id(), targetWkt)
                .orElseThrow(() -> new IllegalStateException("Drone not found: " + drone.id()));

        int newStatus = distance <= stepMeters ? arrivedStatus : inTransitStatus;

        // #region agent log
        try (var fw = new java.io.FileWriter("debug-591cf6.log", true)) {
            fw.write("{\"sessionId\":\"591cf6\",\"hypothesisId\":\"H4\",\"location\":\"MockDeliveryService.moveDrone\",\"message\":\"movement decision\",\"data\":{\"droneId\":" + drone.id() + ",\"distance\":" + distance + ",\"stepMeters\":" + stepMeters + ",\"newStatus\":" + newStatus + ",\"arrivedStatus\":" + arrivedStatus + ",\"inTransitStatus\":" + inTransitStatus + "},\"timestamp\":" + System.currentTimeMillis() + ",\"runId\":\"pre-fix\"}\n");
        } catch (Exception ignored) {}
        // #endregion

        droneRepository.updateDroneMovement(
                drone.id(), targetWkt, pointWkt, stepMeters, batteryDelta, newStatus);
    }
}
