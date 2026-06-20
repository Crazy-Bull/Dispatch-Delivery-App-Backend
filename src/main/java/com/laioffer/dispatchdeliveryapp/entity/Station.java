package com.laioffer.dispatchdeliveryapp.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("stations")
public record Station(
        @Id Long id,
        String name,
        String position,
        String address
) {}
