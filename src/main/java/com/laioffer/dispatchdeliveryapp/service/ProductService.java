package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.dto.ProductResponse;
import com.laioffer.dispatchdeliveryapp.entity.Product;
import com.laioffer.dispatchdeliveryapp.repository.ProductRepository;
import com.laioffer.dispatchdeliveryapp.repository.StationProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StationProductRepository stationProductRepository;

    public ProductService(
            ProductRepository productRepository,
            StationProductRepository stationProductRepository) {
        this.productRepository = productRepository;
        this.stationProductRepository = stationProductRepository;
    }

    public List<ProductResponse> getCatalog() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {
        int maxStock = stationProductRepository.findMaxStockByProductId(product.id());
        return new ProductResponse(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                maxStock,
                product.imageUrl());
    }
}
