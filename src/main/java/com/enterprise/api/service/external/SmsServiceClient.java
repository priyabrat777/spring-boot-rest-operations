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
 * Client for external SMS service integration.
 * Handles SMS delivery through external SMS service providers.
 */
@Service
public class SmsServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(SmsServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${app.external.sms.enabled:false}")
    private boolean smsServiceEnabled;

    public SmsServiceClient(@Qualifier("smsServiceRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendSms(String phoneNumber, String message) {
        if (!smsServiceEnabled) {
            logger.info("SMS service disabled, simulating SMS delivery to: {}", maskPhoneNumber(phoneNumber));
            return true;
        }

        try {
            logger.info("Sending SMS to: {} via external service", maskPhoneNumber(phoneNumber));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                "phoneNumber", phoneNumber,
                "message", message,
                "timestamp", System.currentTimeMillis()
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity("/sms/send", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("SMS sent successfully to: {}", maskPhoneNumber(phoneNumber));
                return true;
            } else {
                logger.error("Failed to send SMS to: {}. Status: {}", maskPhoneNumber(phoneNumber), response.getStatusCode());
                return false;
            }

        } catch (Exception ex) {
            logger.error("Error sending SMS to: {}", maskPhoneNumber(phoneNumber), ex);
            return false;
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return "****";
        }
        return phoneNumber.substring(0, 2) + "****" + phoneNumber.substring(Math.max(2, phoneNumber.length() - 2));
    }
}