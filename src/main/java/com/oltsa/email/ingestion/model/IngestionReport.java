package com.oltsa.email.ingestion.model;

/**
 * A report on the current status of the ingestion process.
 *
 * @param ingestionRunning      True if a process is actively running, otherwise false.
 * @param messagesProcessed     The total number of file entries attempted to be processed from the archive.
 * @param validSenderMessages   The total number of messages that had a successfully extracted sender identifier.
 */
public record IngestionReport(boolean ingestionRunning, int messagesProcessed, int validSenderMessages) {}