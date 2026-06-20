package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.Station;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StationRepository extends ListCrudRepository<Station, Long> {
}
