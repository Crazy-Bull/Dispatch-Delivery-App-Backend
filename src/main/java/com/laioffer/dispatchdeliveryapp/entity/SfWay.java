package com.laioffer.dispatchdeliveryapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("sf_ways")
public record SfWay(
        @Id Integer gid,
        @Column("osm_id") Long osmId,
        String name,
        String highway,
        @Column("maxspeed") Integer maxSpeed,
        @Column("one_way") String oneWay,
        Integer source,
        Integer target,
        Double cost,
        @Column("reverse_cost") Double reverseCost,
        String geom
) {}
