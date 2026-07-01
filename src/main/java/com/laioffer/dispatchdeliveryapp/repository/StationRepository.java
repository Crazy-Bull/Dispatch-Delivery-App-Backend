package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.Station;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StationRepository extends ListCrudRepository<Station, Long> {

    @Query("""
            SELECT ST_AsText(position::geometry)
            FROM stations
            WHERE id = :id
            """)
    Optional<String> findPositionWktById(@Param("id") Long id);

    @Query("""
            SELECT ST_Distance(
                position,
                ST_GeogFromText(:deliveryPositionWkt)
            ) / 1000.0
            FROM stations
            WHERE id = :id
            """)
    Optional<Double> findDistanceKmToPoint(
            @Param("id") Long id,
            @Param("deliveryPositionWkt") String deliveryPositionWkt);
}
