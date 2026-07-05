package com.laioffer.dispatchdeliveryapp.repository;



import com.laioffer.dispatchdeliveryapp.entity.Order;

import org.springframework.data.jdbc.repository.query.Modifying;

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

    @Query("""
            SELECT ST_AsText(delivery_position::geometry)
            FROM orders
            WHERE id = :orderId
            """)
    Optional<String> findDeliveryPositionWktByOrderId(@Param("orderId") Long orderId);

    @Modifying

    @Query("""

            INSERT INTO orders (order_no, user_id, station_id, assigned_drone_id, delivery_position,

                                status, total_amount, assigned_at)

            VALUES (:orderNo, :userId, :stationId, :assignedDroneId, ST_GeogFromText(:deliveryPositionWkt),

                    :status, :totalAmount, CURRENT_TIMESTAMP)

            """)

    void insertOrder(

            @Param("orderNo") String orderNo,

            @Param("userId") Long userId,

            @Param("stationId") Long stationId,

            @Param("assignedDroneId") Long assignedDroneId,

            @Param("deliveryPositionWkt") String deliveryPositionWkt,

            @Param("status") int status,

            @Param("totalAmount") java.math.BigDecimal totalAmount);



    Optional<Order> findByOrderNo(String orderNo);



    @Modifying

    @Query("""

            UPDATE orders SET status = :status, delivered_at = CURRENT_TIMESTAMP

            WHERE assigned_drone_id = :droneId AND status = 1

            """)

    void markDeliveredByDroneId(@Param("droneId") Long droneId, @Param("status") int status);



    @Modifying

    @Query("""

            UPDATE orders SET status = :status, completed_at = CURRENT_TIMESTAMP

            WHERE assigned_drone_id = :droneId AND status = 2

            """)

    void markCompletedByDroneId(@Param("droneId") Long droneId, @Param("status") int status);

}

