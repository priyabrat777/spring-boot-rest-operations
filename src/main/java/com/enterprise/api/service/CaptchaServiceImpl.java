package com.enterprise.api.service;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of CaptchaService that generates visual CAPTCHA challenges
 * and validates user responses using session-based storage.
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {
    
    private static final String CAPTCHA_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CAPTCHA_LENGTH = 5;
    private static final int IMAGE_WIDTH = 200;
    private static final int IMAGE_HEIGHT = 60;
    private static final int CAPTCHA_EXPIRY_MINUTES = 5;
    
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, CaptchaData> captchaStorage = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public CaptchaServiceImpl() {
        // Schedule cleanup of expired CAPTCHAs every minute
        scheduler.scheduleAtFixedRate(this::cleanupExpiredCaptchas, 1, 1, TimeUnit.MINUTES);
    }
    
    @Override
    public BufferedImage generateCaptcha(String sessionId) {
        String captchaText = generateRandomText();
        BufferedImage image = createCaptchaImage(captchaText);
        
        // Store CAPTCHA with expiration time
        long expirationTime = System.currentTimeMillis() + (CAPTCHA_EXPIRY_MINUTES * 60 * 1000);
        captchaStorage.put(sessionId, new CaptchaData(captchaText, expirationTime));
        
        return image;
    }
    
    @Override
    public boolean validateCaptcha(String sessionId, String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        
        CaptchaData captchaData = captchaStorage.get(sessionId);
        if (captchaData == null) {
            return false;
        }
        
        // Check if CAPTCHA has expired
        if (System.currentTimeMillis() > captchaData.getExpirationTime()) {
            captchaStorage.remove(sessionId);
            return false;
        }
        
        // Case-insensitive comparison
        boolean isValid = captchaData.getText().equalsIgnoreCase(response.trim());
        
        // Remove CAPTCHA after validation attempt (one-time use)
        captchaStorage.remove(sessionId);
        
        return isValid;
    }
    
    @Override
    public void clearCaptcha(String sessionId) {
        captchaStorage.remove(sessionId);
    }
    
    @Override
    public String getCaptchaText(String sessionId) {
        CaptchaData captchaData = captchaStorage.get(sessionId);
        return captchaData != null ? captchaData.getText() : null;
    }
    
    private String generateRandomText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            int index = random.nextInt(CAPTCHA_CHARS.length());
            sb.append(CAPTCHA_CHARS.charAt(index));
        }
        return sb.toString();
    }
    
    private BufferedImage createCaptchaImage(String text) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Fill background with white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // Add noise lines
        addNoiseLines(g2d);
        
        // Draw CAPTCHA text
        drawCaptchaText(g2d, text);
        
        // Add noise dots
        addNoiseDots(g2d);
        
        g2d.dispose();
        return image;
    }
    
    private void addNoiseLines(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1.5f));
        
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }
    
    private void drawCaptchaText(Graphics2D g2d, String text) {
        Font[] fonts = {
            new Font("Arial", Font.BOLD, 24),
            new Font("Times New Roman", Font.BOLD, 24),
            new Font("Courier New", Font.BOLD, 24)
        };
        
        Color[] colors = {
            Color.BLACK, Color.BLUE, Color.RED, Color.DARK_GRAY, Color.MAGENTA
        };
        
        int x = 20;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Random font and color for each character
            Font font = fonts[random.nextInt(fonts.length)];
            Color color = colors[random.nextInt(colors.length)];
            
            g2d.setFont(font);
            g2d.setColor(color);
            
            // Random rotation for each character
            double angle = (random.nextDouble() - 0.5) * 0.5; // -0.25 to 0.25 radians
            g2d.rotate(angle, x, IMAGE_HEIGHT / 2);
            
            // Random vertical offset
            int yOffset = random.nextInt(10) - 5;
            g2d.drawString(String.valueOf(c), x, IMAGE_HEIGHT / 2 + 8 + yOffset);
            
            // Reset rotation
            g2d.rotate(-angle, x, IMAGE_HEIGHT / 2);
            
            x += 30;
        }
    }
    
    private void addNoiseDots(Graphics2D g2d) {
        g2d.setColor(Color.GRAY);
        
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(IMAGE_WIDTH);
            int y = random.nextInt(IMAGE_HEIGHT);
            g2d.fillOval(x, y, 2, 2);
        }
    }
    
    private void cleanupExpiredCaptchas() {
        long currentTime = System.currentTimeMillis();
        captchaStorage.entrySet().removeIf(entry -> 
            currentTime > entry.getValue().getExpirationTime());
    }
    
    /**
     * Internal class to store CAPTCHA data with expiration time.
     */
    private static class CaptchaData {
        private final String text;
        private final long expirationTime;
        
        public CaptchaData(String text, long expirationTime) {
            this.text = text;
            this.expirationTime = expirationTime;
        }
        
        public String getText() {
            return text;
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
    }
}