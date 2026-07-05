package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.Product;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends ListCrudRepository<Product, Long> {
}
