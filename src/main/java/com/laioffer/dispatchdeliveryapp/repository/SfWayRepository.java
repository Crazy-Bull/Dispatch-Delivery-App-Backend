package com.laioffer.dispatchdeliveryapp.repository;

import com.laioffer.dispatchdeliveryapp.entity.SfWay;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SfWayRepository extends CrudRepository<SfWay, Integer> {

    /**
     * 1. Primary routing engine endpoint.
     */
    @Query("""
        SELECT ST_AsGeoJSON(ST_LineMerge(ST_Collect(w.geom))) 
        FROM pgr_dijkstra(
            'SELECT gid AS id, source, target, cost, reverse_cost FROM sf_ways WHERE cost IS NOT NULL',
            (SELECT id FROM sf_ways_vertices_pgr ORDER BY the_geom <-> ST_SetSRID(ST_MakePoint(?1, ?2), 4326) LIMIT 1),
            (SELECT id FROM sf_ways_vertices_pgr ORDER BY the_geom <-> ST_SetSRID(ST_MakePoint(?3, ?4), 4326) LIMIT 1),
            true
        ) AS path
        JOIN sf_ways w ON path.edge = w.gid
    """)
    String findRouteAsGeoJson(double startLon, double startLat, double endLon, double endLat);

    /**
     * 2. Data migration from raw staging table.
     */
    @Modifying
    @Query("""
        INSERT INTO sf_ways (osm_id, name, highway, one_way, maxspeed, geom)
        SELECT 
            osm_id, 
            name, 
            highway, 
            'no' AS one_way, 
            30 AS maxspeed,
            ST_Transform(way, 4326) AS geom
        FROM planet_osm_line
        WHERE highway IN ('motorway', 'motorway_link', 'trunk', 'trunk_link', 'primary', 
                          'primary_link', 'secondary', 'secondary_link', 'tertiary', 
                          'tertiary_link', 'residential', 'living_street', 'unclassified')
    """)
    void populateFromPlanetOsmLine();
}
