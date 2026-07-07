package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.StationProduct;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface StationProductRepository extends Repository<StationProduct, Void> {

    @Query("""
            SELECT stock FROM station_products
            WHERE station_id = :stationId AND product_id = :productId
            """)
    Optional<Integer> findStock(@Param("stationId") Long stationId, @Param("productId") Long productId);

    @Query("""
            SELECT COALESCE(MAX(stock), 0) FROM station_products
            WHERE product_id = :productId
            """)
    int findMaxStockByProductId(@Param("productId") Long productId);

    // Catalog for a single hub: product fields + per-station stock.
    // Returns raw rows because Spring Data JDBC does not auto-bind
    // snake_case columns to record components; the controller maps them.
    @Query("""
            SELECT p.id, p.name, p.description, p.price, p.image_url, COALESCE(sp.stock, 0) AS stock
            FROM products p
            JOIN station_products sp ON sp.product_id = p.id
            WHERE sp.station_id = :stationId AND sp.stock > 0
            ORDER BY p.id ASC
            """)
    List<StationCatalogRow> findCatalogForStation(@Param("stationId") Long stationId);

    @Modifying
    @Query("""
            UPDATE station_products SET stock = stock - :quantity
            WHERE station_id = :stationId AND product_id = :productId AND stock >= :quantity
            """)
    int decrementStock(
            @Param("stationId") Long stationId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity);

    // Projection DTO carrying the raw columns from findCatalogForStation.
    // Field names follow the SQL column names so Spring Data JDBC binds
    // rows automatically; the controller converts to ProductResponse.
    record StationCatalogRow(
            Long id,
            String name,
            String description,
            java.math.BigDecimal price,
            String image_url,
            Integer stock
    ) {}
}
