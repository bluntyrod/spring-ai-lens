package io.ailens.springailens.util.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.JdbcTemplate;

import io.ailens.springailens.config.AiLensProperties;
import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.util.EventStore;

public class PostgresEventStore implements EventStore {

    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS ai_lens_events (
            id VARCHAR(36) PRIMARY KEY,
            timestamp TIMESTAMP NOT NULL,
            model VARCHAR(255),
            prompt TEXT,
            response TEXT,
            latency_ms BIGINT,
            prompt_tokens INT,
            completion_tokens INT,
            has_anomaly BOOLEAN,
            anomaly_type VARCHAR(50),
            anomaly_message TEXT,
            prompt_changed BOOLEAN
        )
        """;

    private static final String INSERT = """
        INSERT INTO ai_lens_events
        (id, timestamp, model, prompt, response, latency_ms,
         prompt_tokens, completion_tokens, has_anomaly, anomaly_type,
         anomaly_message, prompt_changed)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO NOTHING
        """;

    private static final String SELECT_ALL = """
        SELECT * FROM ai_lens_events ORDER BY timestamp DESC
        """;

    private static final String SELECT_RECENT = """
        SELECT * FROM ai_lens_events ORDER BY timestamp DESC LIMIT ?
        """;

    private static final String COUNT = "SELECT COUNT(*) FROM ai_lens_events";
    private static final String DELETE_ALL = "DELETE FROM ai_lens_events";

    private final JdbcTemplate jdbc;
    private final AiLensProperties properties;
    private final BlockingQueue<AiCallEvent> writeQueue;
    private final ScheduledExecutorService scheduler;

    public PostgresEventStore(JdbcTemplate jdbc, AiLensProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.writeQueue = new ArrayBlockingQueue<>(10_000);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ai-lens-postgres-writer");
            t.setDaemon(true);
            return t;
        });
        initTable();
        startBatchWriter();
    }

    private void initTable() {
        jdbc.execute(CREATE_TABLE);
    }

    private void startBatchWriter() {
        int intervalMs = properties.getStorage().getPostgres().getFlushIntervalMs();
        scheduler.scheduleAtFixedRate(this::flush, intervalMs, intervalMs,
                TimeUnit.MILLISECONDS);
    }

    private void flush() {
        int batchSize = properties.getStorage().getPostgres().getBatchSize();
        List<AiCallEvent> batch = new ArrayList<>(batchSize);
        writeQueue.drainTo(batch, batchSize);
        if (batch.isEmpty()) return;

        jdbc.batchUpdate(INSERT, batch, batch.size(), (ps, event) -> {
            ps.setString(1, event.id());
            ps.setTimestamp(2, Timestamp.from(event.timestamp()));
            ps.setString(3, event.model());
            ps.setString(4, event.prompt());
            ps.setString(5, event.response());
            ps.setLong(6, event.latencyMs());
            ps.setInt(7, event.promptTokens());
            ps.setInt(8, event.completionTokens());
            ps.setBoolean(9, event.anomaly() != null && event.anomaly().hasAnomaly());
            ps.setString(10, event.anomaly() != null && event.anomaly().type() != null
                    ? event.anomaly().type().name() : null);
            ps.setString(11, event.anomaly() != null ? event.anomaly().message() : null);
            ps.setBoolean(12, event.diff() != null && event.diff().hasChanged());
        });
    }

    @Override
    public void add(AiCallEvent event) {
        if (!writeQueue.offer(event)) {
            writeQueue.poll();
            writeQueue.offer(event);
        }
    }

    @Override
    public List<AiCallEvent> getAll() {
        return jdbc.query(SELECT_ALL, this::mapRow);
    }

    @Override
    public List<AiCallEvent> getRecent(int limit) {
        return jdbc.query(SELECT_RECENT, this::mapRow, limit);
    }

    @Override
    public long count() {
        Long count = jdbc.queryForObject(COUNT, Long.class);
        return count != null ? count : 0;
    }

    @Override
    public void clear() {
        flush();
        jdbc.execute(DELETE_ALL);
    }

    private AiCallEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AiCallEvent(
                rs.getString("id"),
                rs.getTimestamp("timestamp").toInstant(),
                rs.getString("model"),
                rs.getString("prompt"),
                rs.getString("response"),
                rs.getLong("latency_ms"),
                rs.getInt("prompt_tokens"),
                rs.getInt("completion_tokens"),
                null,
                null
        );
    }
}
