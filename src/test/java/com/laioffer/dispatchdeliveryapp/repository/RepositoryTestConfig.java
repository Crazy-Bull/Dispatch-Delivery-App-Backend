package com.laioffer.dispatchdeliveryapp.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;

import java.util.List;

@TestConfiguration
class RepositoryTestConfig {

    @Bean
    JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(List.of(new PgObjectToStringConverter()));
    }
}
