package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.Order;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends ListCrudRepository<Order, Long> {
}
