package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.OrderItem;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends ListCrudRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
}
