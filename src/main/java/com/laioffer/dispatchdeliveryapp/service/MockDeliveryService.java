package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.config.MockDeliveryProperties;
import com.laioffer.dispatchdeliveryapp.entity.Drone;
import com.laioffer.dispatchdeliveryapp.model.DroneStatus;
import com.laioffer.dispatchdeliveryapp.model.OrderStatus;
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

        if (targetPointWkt == null) {
            log.warn("Drone {} is in delivery mode but has no assigned order", drone.droneCode());
            return;
        }

        boolean arrived = moveDrone(drone, targetPointWkt, stepMeters, -1, DroneStatus.RETURN, DroneStatus.DELIVERY);
        if (arrived) {
            orderRepository.markDeliveredByDroneId(drone.id(), OrderStatus.DELIVERED);
        }
    }

    private void processReturn(Drone drone, double stepMeters) {
        String targetPointWkt = stationRepository.findPositionWktById(drone.stationId())
                .orElse(null);
        if (targetPointWkt == null) {
            log.warn("Drone {} has unknown station {}", drone.droneCode(), drone.stationId());
            return;
        }

        boolean arrived = moveDrone(drone, targetPointWkt, stepMeters, -1, DroneStatus.WAITING, DroneStatus.RETURN);
        if (arrived) {
            orderRepository.markCompletedByDroneId(drone.id(), OrderStatus.COMPLETED);
        }
    }

    private boolean moveDrone(
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

        boolean arrived = distance <= stepMeters;
        int newStatus = arrived ? arrivedStatus : inTransitStatus;

        droneRepository.updateDroneMovement(
                drone.id(), targetWkt, pointWkt, stepMeters, batteryDelta, newStatus);

        return arrived;
    }
}
