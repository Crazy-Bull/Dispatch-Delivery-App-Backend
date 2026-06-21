package com.laioffer.dispatchdeliveryapp.repository;

import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJdbcTest
@EnableJdbcRepositories(basePackageClasses = DroneRepository.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractPostgresRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgrouting/pgrouting:16-3.5-4.0").asCompatibleSubstituteFor("postgres"))
            .withUsername("postgres")
            .withPassword("secret");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected Long missionHubStationId(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM stations WHERE name = 'Mission Hub'",
                Long.class);
    }

    protected static String pointWkt(double lon, double lat) {
        return "SRID=4326;POINT(%f %f)".formatted(lon, lat);
    }
}
