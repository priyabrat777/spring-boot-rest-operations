package com.enterprise.api.service.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for external email service integration.
 * Handles email delivery through external email service providers.
 */
@Service
public class EmailServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${app.external.email.enabled:false}")
    private boolean emailServiceEnabled;

    public EmailServiceClient(@Qualifier("emailServiceRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendEmail(String recipient, String subject, String message) {
        if (!emailServiceEnabled) {
            logger.info("Email service disabled, simulating email delivery to: {}", maskEmail(recipient));
            return true;
        }

        try {
            logger.info("Sending email to: {} via external service", maskEmail(recipient));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                "recipient", recipient,
                "subject", subject,
                "message", message,
                "timestamp", System.currentTimeMillis()
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity("/email/send", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Email sent successfully to: {}", maskEmail(recipient));
                return true;
            } else {
                logger.error("Failed to send email to: {}. Status: {}", maskEmail(recipient), response.getStatusCode());
                return false;
            }

        } catch (Exception ex) {
            logger.error("Error sending email to: {}", maskEmail(recipient), ex);
            return false;
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "**@" + domain;
        } else {
            return localPart.substring(0, 2) + "****@" + domain;
        }
    }
}