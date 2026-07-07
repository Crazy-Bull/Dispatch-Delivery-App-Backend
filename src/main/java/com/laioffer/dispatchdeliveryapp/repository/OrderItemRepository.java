package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.dto.OrderItemWithProduct;
import com.laioffer.dispatchdeliveryapp.entity.OrderItem;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends ListCrudRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // Fetch order line items joined with product name + image so the UI
    // can render the receipt without a second round-trip per item.
    @Query("""
            SELECT oi.product_id, p.name, p.image_url, oi.quantity, oi.unit_price
            FROM order_items oi
            JOIN products p ON p.id = oi.product_id
            WHERE oi.order_id = :orderId
            """)
    List<OrderItemWithProduct> findWithProductByOrderId(@Param("orderId") Long orderId);
}
