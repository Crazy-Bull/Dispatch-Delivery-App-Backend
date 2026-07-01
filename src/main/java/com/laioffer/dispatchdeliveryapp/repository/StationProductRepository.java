package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.StationProduct;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("""
            UPDATE station_products SET stock = stock - :quantity
            WHERE station_id = :stationId AND product_id = :productId AND stock >= :quantity
            """)
    int decrementStock(
            @Param("stationId") Long stationId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity);
}
