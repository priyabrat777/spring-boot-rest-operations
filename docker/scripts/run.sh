#!/bin/bash

# Docker run script for Enterprise Spring Boot API
# This script runs the application with proper configuration

set -e

# Configuration
IMAGE_NAME="enterprise-api"
CONTAINER_NAME="enterprise-api-container"
DEFAULT_PORT=8080

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Default values
PORT=$DEFAULT_PORT
PROFILE="dev"
DETACHED=false
REMOVE=false
VERSION="latest"
NETWORK=""
VOLUMES=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        -d|--detach)
            DETACHED=true
            shift
            ;;
        --rm)
            REMOVE=true
            shift
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --network)
            NETWORK="$2"
            shift 2
            ;;
        -v|--volume)
            VOLUMES="$VOLUMES -v $2"
            shift 2
            ;;
        --stop)
            print_status "Stopping container $CONTAINER_NAME..."
            docker stop $CONTAINER_NAME 2>/dev/null || print_warning "Container not running"
            docker rm $CONTAINER_NAME 2>/dev/null || print_warning "Container not found"
            print_status "Container stopped and removed"
            exit 0
            ;;
        --logs)
            print_status "Showing logs for container $CONTAINER_NAME..."
            docker logs -f $CONTAINER_NAME
            exit 0
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -p, --port PORT     Port to expose (default: $DEFAULT_PORT)"
            echo "  --profile PROFILE   Spring profile to use (default: dev)"
            echo "  -d, --detach        Run container in background"
            echo "  --rm                Remove container when it exits"
            echo "  --version VERSION   Image version to run (default: latest)"
            echo "  --network NETWORK   Docker network to use"
            echo "  -v, --volume VOL    Add volume mount (can be used multiple times)"
            echo "  --stop              Stop and remove running container"
            echo "  --logs              Show container logs"
            echo "  -h, --help          Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                                    # Run with defaults"
            echo "  $0 -p 8081 --profile prod -d         # Run on port 8081 with prod profile in background"
            echo "  $0 -v ./logs:/app/logs --rm           # Run with volume mount and auto-remove"
            echo "  $0 --stop                             # Stop running container"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if image exists
if ! docker images | grep -q "$IMAGE_NAME.*$VERSION"; then
    print_error "Image $IMAGE_NAME:$VERSION not found. Please build it first using:"
    print_error "  ./docker/scripts/build.sh"
    exit 1
fi

# Stop existing container if running
if docker ps | grep -q $CONTAINER_NAME; then
    print_warning "Container $CONTAINER_NAME is already running. Stopping it..."
    docker stop $CONTAINER_NAME
fi

# Remove existing container if exists
if docker ps -a | grep -q $CONTAINER_NAME; then
    print_warning "Removing existing container $CONTAINER_NAME..."
    docker rm $CONTAINER_NAME
fi

# Build docker run command
DOCKER_RUN_CMD="docker run"

# Add run options
if [ "$DETACHED" = true ]; then
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -d"
else
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD -it"
fi

if [ "$REMOVE" = true ]; then
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD --rm"
fi

# Add container name
DOCKER_RUN_CMD="$DOCKER_RUN_CMD --name $CONTAINER_NAME"

# Add port mapping
DOCKER_RUN_CMD="$DOCKER_RUN_CMD -p $PORT:8080"

# Add network if specified
if [ -n "$NETWORK" ]; then
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD --network $NETWORK"
fi

# Add volumes
if [ -n "$VOLUMES" ]; then
    DOCKER_RUN_CMD="$DOCKER_RUN_CMD $VOLUMES"
fi

# Add default volumes for logs and uploads
DOCKER_RUN_CMD="$DOCKER_RUN_CMD -v $(pwd)/logs:/app/logs -v $(pwd)/test-uploads:/app/uploads"

# Add environment variables
DOCKER_RUN_CMD="$DOCKER_RUN_CMD \
    -e SPRING_PROFILES_ACTIVE=$PROFILE \
    -e SERVER_PORT=8080"

# Add health check environment
DOCKER_RUN_CMD="$DOCKER_RUN_CMD \
    -e MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=always \
    -e MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics"

# Add image
DOCKER_RUN_CMD="$DOCKER_RUN_CMD $IMAGE_NAME:$VERSION"

print_status "Starting Enterprise Spring Boot API container"
print_status "Image: $IMAGE_NAME:$VERSION"
print_status "Port: $PORT"
print_status "Profile: $PROFILE"
print_status "Container: $CONTAINER_NAME"

# Create directories if they don't exist
mkdir -p logs test-uploads

print_status "Executing: $DOCKER_RUN_CMD"

# Execute the run command
if eval $DOCKER_RUN_CMD; then
    if [ "$DETACHED" = true ]; then
        print_status "Container started successfully in background!"
        print_status "Application will be available at: http://localhost:$PORT"
        print_status "Health check: http://localhost:$PORT/actuator/health"
        print_status "API docs: http://localhost:$PORT/swagger-ui.html"
        print_status ""
        print_status "To view logs: $0 --logs"
        print_status "To stop container: $0 --stop"
    else
        print_status "Container started successfully!"
    fi
else
    print_error "Failed to start container!"
    exit 1
fi