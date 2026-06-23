package io.ailens.springailens.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.ailens.springailens.config.AiLensProperties;
import io.ailens.springailens.config.PostgresStorageProperties;
import io.ailens.springailens.config.StorageProperties;
import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.model.AnomalyReport;
import io.ailens.springailens.model.StorageType;
import io.ailens.springailens.util.store.PostgresEventStore;

@Disabled
@Testcontainers
class PostgresEventStoreTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ailens_test")
            .withUsername("test")
            .withPassword("test");

    private PostgresEventStore store;

    @BeforeEach
    void setUp() throws InterruptedException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        AiLensProperties properties = buildProperties();
        store = new PostgresEventStore(jdbc, properties);

        // allow first flush cycle
        Thread.sleep(200);
    }

    @AfterEach
    void tearDown() {
        store.clear();
    }

    private AiCallEvent event(String prompt) {
        return new AiCallEvent(
                UUID.randomUUID().toString(), Instant.now(),
                "OllamaChatModel", prompt, "response",
                100, 10, 20, AnomalyReport.none(), null
        );
    }

    private AiLensProperties buildProperties() {
        AiLensProperties properties = new AiLensProperties();
        StorageProperties storage = new StorageProperties();
        storage.setType(StorageType.POSTGRES);
        PostgresStorageProperties pgProps = new PostgresStorageProperties();
        pgProps.setBatchSize(10);
        pgProps.setFlushIntervalMs(50);
        pgProps.setMaxEvents(100);
        storage.setPostgres(pgProps);
        properties.setStorage(storage);
        return properties;
    }

    @Test
    void addsAndRetrievesEvents() throws InterruptedException {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));
        Thread.sleep(200); // wait for batch flush

        assertThat(store.getAll()).hasSize(2);
        assertThat(store.count()).isEqualTo(2);
    }

    @Test
    void getRecentReturnsLastN() throws InterruptedException {
        store.add(event("prompt 1"));
        store.add(event("prompt 2"));
        store.add(event("prompt 3"));
        Thread.sleep(200);

        List<AiCallEvent> recent = store.getRecent(2);
        assertThat(recent).hasSize(2);
    }

    @Test
    void clearDeletesAllEvents() throws InterruptedException {
        store.add(event("prompt 1"));
        Thread.sleep(200);
        store.clear();

        assertThat(store.getAll()).isEmpty();
        assertThat(store.count()).isEqualTo(0);
    }

    @Test
    void handlesMultipleEventsCorrectly() throws InterruptedException {
        for (int i = 1; i <= 5; i++) {
            store.add(event("prompt " + i));
        }
        Thread.sleep(200);

        assertThat(store.count()).isEqualTo(5);
        assertThat(store.getAll())
                .extracting(AiCallEvent::model)
                .containsOnly("OllamaChatModel");
    }
}
