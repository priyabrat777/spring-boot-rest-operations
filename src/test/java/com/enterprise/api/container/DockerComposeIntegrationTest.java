package com.enterprise.api.container;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Docker Compose configuration.
 * Tests the Docker Compose setup and configuration files.
 */
class DockerComposeIntegrationTest {

    private static final String APP_SERVICE = "enterprise-api";
    private static final String DB_SERVICE = "postgres";
    private static final String REDIS_SERVICE = "redis";



    @Test
    void testDockerComposeFileExists() {
        java.io.File dockerCompose = new java.io.File("docker-compose.yml");
        assertTrue(dockerCompose.exists(), "docker-compose.yml should exist");
        assertTrue(dockerCompose.length() > 0, "docker-compose.yml should not be empty");
    }

    @Test
    void testDockerComposeOverrideExists() {
        java.io.File dockerComposeOverride = new java.io.File("docker-compose.override.yml");
        assertTrue(dockerComposeOverride.exists(), "docker-compose.override.yml should exist");
        assertTrue(dockerComposeOverride.length() > 0, "docker-compose.override.yml should not be empty");
    }

    @Test
    void testDockerComposeProdExists() {
        java.io.File dockerComposeProd = new java.io.File("docker-compose.prod.yml");
        assertTrue(dockerComposeProd.exists(), "docker-compose.prod.yml should exist");
        assertTrue(dockerComposeProd.length() > 0, "docker-compose.prod.yml should not be empty");
    }

    @Test
    void testDockerComposeConfiguration() throws IOException {
        // Read and verify docker-compose.yml contains required services
        java.io.File dockerCompose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(dockerCompose.toPath());
        
        assertTrue(content.contains(APP_SERVICE), "Docker compose should contain application service");
        assertTrue(content.contains(DB_SERVICE), "Docker compose should contain database service");
        assertTrue(content.contains(REDIS_SERVICE), "Docker compose should contain Redis service");
        assertTrue(content.contains("version:"), "Docker compose should have version specified");
        assertTrue(content.contains("networks:"), "Docker compose should define networks");
        assertTrue(content.contains("volumes:"), "Docker compose should define volumes");
    }

    @Test
    void testDockerComposeServicePorts() throws IOException {
        // Verify that services expose correct ports
        java.io.File dockerCompose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(dockerCompose.toPath());
        
        assertTrue(content.contains("8080:8080"), "Application should expose port 8080");
        assertTrue(content.contains("5432:5432"), "PostgreSQL should expose port 5432");
        assertTrue(content.contains("6379:6379"), "Redis should expose port 6379");
    }

    @Test
    void testDockerComposeEnvironmentVariables() throws IOException {
        // Verify environment variables are configured
        java.io.File dockerCompose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(dockerCompose.toPath());
        
        assertTrue(content.contains("SPRING_PROFILES_ACTIVE"), "Should configure Spring profiles");
        assertTrue(content.contains("SPRING_DATASOURCE_URL"), "Should configure database URL");
        assertTrue(content.contains("SPRING_REDIS_HOST"), "Should configure Redis host");
    }

    @Test
    void testDockerComposeHealthChecks() throws IOException {
        // Verify health checks are configured
        java.io.File dockerCompose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(dockerCompose.toPath());
        
        assertTrue(content.contains("healthcheck:"), "Services should have health checks");
        assertTrue(content.contains("actuator/health"), "Application should have health check endpoint");
    }

    @Test
    void testDockerComposeDependencies() throws IOException {
        // Verify service dependencies are configured
        java.io.File dockerCompose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(dockerCompose.toPath());
        
        assertTrue(content.contains("depends_on:"), "Application should depend on other services");
        assertTrue(content.contains("condition: service_healthy"), "Should wait for services to be healthy");
    }

    @Test
    void testDockerComposeVolumes() throws IOException {
        // Verify volumes are configured
        java.io.File dockerCompose = new java.io.File("docker-compose.yml");
        String content = java.nio.file.Files.readString(dockerCompose.toPath());
        
        assertTrue(content.contains("./logs:/app/logs"), "Should mount logs volume");
        assertTrue(content.contains("./test-uploads:/app/uploads"), "Should mount uploads volume");
        assertTrue(content.contains("postgres-data:"), "Should define PostgreSQL data volume");
        assertTrue(content.contains("redis-data:"), "Should define Redis data volume");
    }
}