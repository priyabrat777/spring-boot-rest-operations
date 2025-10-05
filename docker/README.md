# Docker Configuration for Enterprise Spring Boot API

This directory contains Docker configuration files and scripts for containerizing and running the Enterprise Spring Boot API application.

## Quick Start

### Prerequisites

- Docker 20.10+ installed
- Docker Compose 2.0+ installed
- At least 4GB of available RAM
- At least 2GB of available disk space

### Build and Run

1. **Build the Docker image:**
   ```bash
   ./docker/scripts/build.sh
   ```

2. **Run with Docker Compose (recommended):**
   ```bash
   docker-compose up -d
   ```

3. **Access the application:**
   - API: http://localhost:8080
   - Health Check: http://localhost:8080/actuator/health
   - API Documentation: http://localhost:8080/swagger-ui.html
   - Database Admin (Adminer): http://localhost:8081
   - Redis Admin: http://localhost:8082

## Docker Files Overview

### Core Files

- **`Dockerfile`** - Multi-stage build configuration for the application
- **`docker-compose.yml`** - Main Docker Compose configuration
- **`docker-compose.override.yml`** - Development environment overrides
- **`docker-compose.prod.yml`** - Production environment configuration
- **`.dockerignore`** - Files to exclude from Docker build context

### Configuration Files

- **`docker/postgres/init/`** - PostgreSQL initialization scripts
- **`docker/redis/redis.conf`** - Redis configuration
- **`.env.template`** - Environment variables template

### Scripts

- **`docker/scripts/build.sh`** - Docker image build script
- **`docker/scripts/run.sh`** - Container run script

## Usage

### Development Environment

The default Docker Compose setup is configured for development:

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f enterprise-api

# Stop all services
docker-compose down

# Rebuild and restart
docker-compose up -d --build
```

### Production Environment

For production deployment:

```bash
# Create environment file
cp .env.template .env
# Edit .env with production values

# Start with production configuration
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Scale the application (if needed)
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale enterprise-api=3
```

### Individual Container Management

```bash
# Build image
./docker/scripts/build.sh --version 1.0.0

# Run container
./docker/scripts/run.sh --port 8080 --profile dev -d

# Stop container
./docker/scripts/run.sh --stop

# View logs
./docker/scripts/run.sh --logs
```

## Configuration

### Environment Variables

Key environment variables for the application:

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `SPRING_DATASOURCE_URL` | Database connection URL | PostgreSQL container |
| `SPRING_REDIS_HOST` | Redis host | Redis container |
| `JWT_SECRET` | JWT signing secret | (required) |
| `DB_PASSWORD` | Database password | (required) |
| `REDIS_PASSWORD` | Redis password | (optional) |

### Volumes

The application uses the following volumes:

- **`./logs:/app/logs`** - Application logs
- **`./test-uploads:/app/uploads`** - File uploads
- **`postgres-data`** - PostgreSQL data persistence
- **`redis-data`** - Redis data persistence

### Ports

Default port mappings:

- **8080** - Application HTTP port
- **5432** - PostgreSQL database
- **6379** - Redis cache
- **8081** - Adminer (database admin)
- **8082** - Redis Commander

## Security Features

### Container Security

- **Non-root user**: Application runs as user `appuser` (UID 1001)
- **Minimal base image**: Uses Alpine Linux for smaller attack surface
- **Read-only filesystem**: Application directories have appropriate permissions
- **Health checks**: Built-in health monitoring
- **Signal handling**: Proper signal handling with dumb-init

### Network Security

- **Internal network**: Services communicate on isolated Docker network
- **Port exposure**: Only necessary ports are exposed to host
- **Environment isolation**: Separate configurations for dev/prod

## Monitoring and Health Checks

### Application Health

The application includes comprehensive health checks:

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Redis health
curl http://localhost:8080/actuator/health/redis

# Application metrics
curl http://localhost:8080/actuator/metrics
```

### Container Health

Docker health checks are configured for all services:

```bash
# Check container health
docker-compose ps

# View health check logs
docker inspect enterprise-api --format='{{json .State.Health}}'
```

## Troubleshooting

### Common Issues

1. **Port conflicts:**
   ```bash
   # Change ports in docker-compose.yml or use different ports
   docker-compose up -d --scale enterprise-api=1
   ```

2. **Memory issues:**
   ```bash
   # Increase Docker memory limit or adjust JVM settings
   export JAVA_OPTS="-Xmx512m -XX:MaxRAMPercentage=50.0"
   ```

3. **Database connection issues:**
   ```bash
   # Check database container logs
   docker-compose logs postgres
   
   # Verify database is ready
   docker-compose exec postgres pg_isready -U enterprise_user
   ```

4. **Permission issues:**
   ```bash
   # Fix volume permissions
   sudo chown -R 1001:1001 logs test-uploads
   ```

### Debugging

Enable debug mode:

```bash
# Set debug environment variables
export SPRING_PROFILES_ACTIVE=dev
export LOGGING_LEVEL_COM_ENTERPRISE=DEBUG

# Run with debug port exposed
./docker/scripts/run.sh --port 8080 -v $(pwd)/src:/app/src
```

### Log Analysis

```bash
# Application logs
docker-compose logs -f enterprise-api

# Database logs
docker-compose logs -f postgres

# Redis logs
docker-compose logs -f redis

# All services logs
docker-compose logs -f
```

## Performance Tuning

### JVM Tuning

The Dockerfile includes optimized JVM settings:

```dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"
```

### Database Tuning

PostgreSQL is configured with:

- Connection pooling via HikariCP
- Optimized memory settings
- Proper indexing strategies

### Redis Tuning

Redis configuration includes:

- Memory management policies
- Persistence settings
- Connection optimization

## CI/CD Integration

### Jenkins Pipeline

The application includes Jenkins pipeline integration:

```groovy
stage('Docker Build') {
    steps {
        script {
            sh './docker/scripts/build.sh --version ${BUILD_NUMBER}'
        }
    }
}

stage('Docker Test') {
    steps {
        script {
            sh 'docker-compose -f docker-compose.test.yml up --abort-on-container-exit'
        }
    }
}
```

### GitHub Actions

Example workflow for GitHub Actions:

```yaml
- name: Build Docker image
  run: ./docker/scripts/build.sh --version ${{ github.sha }}

- name: Run tests
  run: docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

## Best Practices

1. **Use multi-stage builds** to minimize image size
2. **Run as non-root user** for security
3. **Use health checks** for monitoring
4. **Implement proper logging** with structured format
5. **Use secrets management** for sensitive data
6. **Monitor resource usage** and set limits
7. **Regular security updates** for base images
8. **Backup data volumes** regularly

## Support

For issues and questions:

1. Check the application logs: `docker-compose logs enterprise-api`
2. Verify all services are healthy: `docker-compose ps`
3. Review the troubleshooting section above
4. Check the main application README.md for additional information