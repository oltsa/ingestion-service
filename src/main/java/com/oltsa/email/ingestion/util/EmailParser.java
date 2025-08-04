package com.oltsa.email.ingestion.util;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class EmailParser {

    private static final Session SESSION = Session.getDefaultInstance(new Properties());

    /**
     * Parses an email message from an InputStream to extract the sender's address.
     * @param emailStream The InputStream of a single email file.
     * @return An Optional containing the sender's email address.
     * @throws MessagingException if the email stream is malformed or cannot be parsed.
     */
    public static Optional<String> extractSender(InputStream emailStream) throws MessagingException {
        MimeMessage message = new MimeMessage(SESSION, emailStream);

        Address[] fromAddresses = message.getFrom();

        if (fromAddresses == null || fromAddresses.length == 0) {
            return Optional.empty();
        }

        String rawFrom = fromAddresses[0].toString();

        if (rawFrom.contains("<") && rawFrom.contains(">")) {
            int start = rawFrom.lastIndexOf('<');
            int end = rawFrom.lastIndexOf('>');
            if (start != -1 && end != -1 && start < end) {
                return Optional.of(rawFrom.substring(start + 1, end));
            }
        }
        
        return Optional.of(rawFrom);
    }
}