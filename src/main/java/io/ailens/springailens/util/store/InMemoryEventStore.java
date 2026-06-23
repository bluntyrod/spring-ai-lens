package io.ailens.springailens.util.store;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import io.ailens.springailens.model.AiCallEvent;
import io.ailens.springailens.util.EventStore;

public class InMemoryEventStore implements EventStore {

    private final int capacity;
    private final Deque<AiCallEvent> buffer;

    public InMemoryEventStore(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayDeque<>(capacity);
    }

    @Override
    public synchronized void add(AiCallEvent event) {
        if (buffer.size() >= capacity) {
            buffer.pollFirst();
        }
        buffer.addLast(event);
    }

    @Override
    public synchronized List<AiCallEvent> getAll() {
        return List.copyOf(buffer);
    }

    @Override
    public synchronized List<AiCallEvent> getRecent(int limit) {
        return buffer.stream()
                .skip(Math.max(0, buffer.size() - limit))
                .toList();
    }

    @Override
    public synchronized long count() {
        return buffer.size();
    }

    @Override
    public synchronized void clear() {
        buffer.clear();
    }
}
