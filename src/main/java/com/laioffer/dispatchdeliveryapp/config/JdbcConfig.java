package com.laioffer.dispatchdeliveryapp.config;

import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;

import java.util.List;

@Configuration
public class JdbcConfig {

    @Bean
    JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(List.of(new PgObjectToStringConverter()));
    }

    @ReadingConverter
    static class PgObjectToStringConverter implements Converter<PGobject, String> {

        @Override
        public String convert(PGobject source) {
            return source.getValue();
        }
    }
}
