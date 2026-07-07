package com.laioffer.dispatchdeliveryapp.controller;

import com.laioffer.dispatchdeliveryapp.dto.ProductResponse;
import com.laioffer.dispatchdeliveryapp.repository.StationProductRepository;
import com.laioffer.dispatchdeliveryapp.repository.StationProductRepository.StationCatalogRow;
import com.laioffer.dispatchdeliveryapp.repository.StationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/stations")
public class StationController {

    private final StationRepository stationRepository;
    private final StationProductRepository stationProductRepository;

    public StationController(
            StationRepository stationRepository,
            StationProductRepository stationProductRepository) {
        this.stationRepository = stationRepository;
        this.stationProductRepository = stationProductRepository;
    }

    // GET /stations/{stationId}/products
    // Returns the per-hub product catalog with stock. Used by the
    // frontend CategoryPage once a hub is selected, so each hub shows
    // its real inventory instead of the global /products list.
    @GetMapping("/{stationId}/products")
    public ResponseEntity<List<ProductResponse>> getProductsForStation(
            @PathVariable Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new NoSuchElementException("Station not found: " + stationId);
        }
        List<ProductResponse> result = stationProductRepository
                .findCatalogForStation(stationId)
                .stream()
                .map(StationController::toProductResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    private static ProductResponse toProductResponse(StationCatalogRow row) {
        return new ProductResponse(
                row.id(),
                row.name(),
                row.description(),
                row.price(),
                row.stock(),
                row.image_url());
    }
}