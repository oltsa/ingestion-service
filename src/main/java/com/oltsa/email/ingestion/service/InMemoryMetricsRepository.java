package com.oltsa.email.ingestion.service;

import com.oltsa.email.ingestion.model.IngestionReport;
import com.oltsa.email.ingestion.model.SenderMetrics;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A thread-safe, in-memory implementation of the MetricsRepository.
 * For this PoC and for environments where state does not need to be durable.
 * This should be replaced by persistant storage for further use (redis, firestore)
 */
@Repository("inMemoryMetricsRepository")
public class InMemoryMetricsRepository implements MetricsRepository {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger messagesProcessed = new AtomicInteger(0);
    private final AtomicInteger validSenderMessages = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> senderCounts = new ConcurrentHashMap<>();

    @Override
    public boolean tryStartIngestion() {
        return isRunning.compareAndSet(false, true);
    }

    @Override
    public void finishIngestion() {
        isRunning.set(false);
    }

    @Override
    public void reset() {
        messagesProcessed.set(0);
        validSenderMessages.set(0);
        senderCounts.clear();
    }
    
    @Override
    public void incrementMessagesProcessed() {
        messagesProcessed.incrementAndGet();
    }

    @Override
    public void incrementValidSenderMessages(String sender) {
        validSenderMessages.incrementAndGet();
        senderCounts.computeIfAbsent(sender, k -> new AtomicInteger(0)).incrementAndGet();
    }

    @Override
    public IngestionReport getCurrentStatus() {
        return new IngestionReport(isRunning.get(), messagesProcessed.get(), validSenderMessages.get());
    }

    @Override
    public List<SenderMetrics> getTopSenders(int limit) {
        return senderCounts.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().get(), e1.getValue().get()))
                .limit(limit)
                .map(entry -> new SenderMetrics(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList());
    }
}