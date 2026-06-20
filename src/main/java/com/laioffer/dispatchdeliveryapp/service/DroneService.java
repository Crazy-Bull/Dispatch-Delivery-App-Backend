package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.dto.AddDroneRequest;
import com.laioffer.dispatchdeliveryapp.entity.Drone;
import com.laioffer.dispatchdeliveryapp.repository.DroneRepository;
import com.laioffer.dispatchdeliveryapp.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DroneService {

    private final DroneRepository droneRepository;
    private final StationRepository stationRepository;

    public DroneService(DroneRepository droneRepository, StationRepository stationRepository) {
        this.droneRepository = droneRepository;
        this.stationRepository = stationRepository;
    }

    public List<Drone> getAllDrones() {
        return droneRepository.findAll();
    }

    public Drone getById(Long id) {
        return droneRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Drone not found: " + id));
    }

    public List<Drone> findByStationIdStatusAndMinBatteryLevel(
            Long stationId, Integer status, Integer minBatteryLevel) {
        return droneRepository.findByStationIdAndStatusAndMinBatteryLevel(stationId, status, minBatteryLevel);
    }

    @Transactional
    public Drone addDrone(AddDroneRequest request) {
        if (!stationRepository.existsById(request.stationId())) {
            throw new IllegalArgumentException("Station not found: " + request.stationId());
        }
        if (droneRepository.findByDroneCode(request.droneCode()).isPresent()) {
            throw new IllegalArgumentException("Drone code already exists: " + request.droneCode());
        }

        int batteryLevel = request.batteryLevel() != null ? request.batteryLevel() : 100;
        double altitude = request.altitude() != null ? request.altitude() : 0.0;
        double speed = request.speed() != null ? request.speed() : 0.0;
        int status = request.status() != null ? request.status() : 0;
        String positionWkt = "SRID=4326;POINT(%f %f)".formatted(request.longitude(), request.latitude());

        droneRepository.insertDrone(
                request.droneCode(),
                request.stationId(),
                batteryLevel,
                positionWkt,
                altitude,
                speed,
                status);

        return droneRepository.findByDroneCode(request.droneCode())
                .orElseThrow(() -> new IllegalStateException("Failed to load drone after insert: " + request.droneCode()));
    }

    @Transactional
    public void deleteDrone(Long id) {
        if (!droneRepository.existsById(id)) {
            throw new NoSuchElementException("Drone not found: " + id);
        }
        droneRepository.deleteById(id);
    }
}
