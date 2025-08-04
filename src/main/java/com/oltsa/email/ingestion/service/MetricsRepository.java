package com.oltsa.email.ingestion.service;

import com.oltsa.email.ingestion.model.IngestionReport;
import com.oltsa.email.ingestion.model.SenderMetrics;

import java.util.List;

/**
 * An interface defining the contract for storing and retrieving ingestion metrics.
 * This decouples the service logic from the underlying storage mechanism.
 */
public interface MetricsRepository {
    /**
     * Marks the beginning of an ingestion process.
     * @return true if the process was started, false if one was already running.
     */
    boolean tryStartIngestion();

    /**
     * Marks the end of an ingestion process.
     */
    void finishIngestion();

    /**
     * Resets all metrics to their initial state for a new run.
     */
    void reset();

    /**
     * Increments the total count of message files attempted to be processed.
     */
    void incrementMessagesProcessed();

    /**
     * Increments the count of messages with a valid sender and updates that sender's total count.
     * @param sender The sender identifier found in the message.
     */
    void incrementValidSenderMessages(String sender);

    /**
     * Returns the current status of the ingestion.
     * @return An IngestionReport containing the current status.
     */
    IngestionReport getCurrentStatus();

    /**
     * Returns the top N senders by message count.
     * @param limit The number of top senders to return.
     * @return A list of sender metrics.
     */
    List<SenderMetrics> getTopSenders(int limit);
}