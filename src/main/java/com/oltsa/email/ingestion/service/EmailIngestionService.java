package com.oltsa.email.ingestion.service;

import com.oltsa.email.ingestion.model.IngestionReport;
import com.oltsa.email.ingestion.model.SenderMetrics;
import com.oltsa.email.ingestion.util.EmailParser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Service
public class EmailIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EmailIngestionService.class);

    private final MetricsRepository metricsRepository;

    public EmailIngestionService(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    public void startIngestion(InputStream inputStream) {
        if (metricsRepository.tryStartIngestion()) {
            metricsRepository.reset();
            processArchive(inputStream);
        } else {
            throw new IllegalStateException("Ingestion is already in progress.");
        }
    }

    public IngestionReport getStatus() {
        return metricsRepository.getCurrentStatus();
    }

    public List<SenderMetrics> getTopSenders(int limit) {
        return metricsRepository.getTopSenders(limit);
    }

    @Async
    public void processArchive(InputStream inputStream) {
        log.info("Starting archive processing on a background thread.");

        try (
            GZIPInputStream gis = new GZIPInputStream(inputStream);
            TarArchiveInputStream tis = new TarArchiveInputStream(gis)
        ) {
            ArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (!(entry instanceof TarArchiveEntry tarEntry && tarEntry.isFile())) {
                    continue;
                }

                metricsRepository.incrementMessagesProcessed();

                try {
                    EmailParser.extractSender(tis).ifPresent(
                        sender -> metricsRepository.incrementValidSenderMessages(sender)
                    );
                } catch (jakarta.mail.MessagingException e) {
                    log.warn("Could not parse entry '{}' due to malformed data. Reason: {}", entry.getName(), e.getMessage());
                } catch (Exception e) {
                    log.error("An unexpected error occurred while processing entry: {}. Skipping.", entry.getName(), e);
                }
            }
        } catch (IOException e) {
            log.error("A critical error occurred while reading the main archive stream. Ingestion stopped.", e);
        } finally {
            metricsRepository.finishIngestion();
            IngestionReport finalReport = metricsRepository.getCurrentStatus();
            log.info("Finished archive processing. Attempted to process {} files, from which {} had a valid sender.",
                     finalReport.messagesProcessed(), finalReport.validSenderMessages());
        }
    }
}