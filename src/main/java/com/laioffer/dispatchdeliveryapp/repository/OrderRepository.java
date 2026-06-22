package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.Order;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends ListCrudRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
            SELECT ST_AsText(delivery_position::geometry)
            FROM orders
            WHERE assigned_drone_id = :droneId
            ORDER BY assigned_at DESC NULLS LAST, id DESC
            LIMIT 1
            """)
    Optional<String> findDeliveryPositionWktByAssignedDroneId(@Param("droneId") Long droneId);
}
