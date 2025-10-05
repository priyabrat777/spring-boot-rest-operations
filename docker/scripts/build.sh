#!/bin/bash

# Docker build script for Enterprise Spring Boot API
# This script builds the Docker image with proper tagging and build args

set -e

# Configuration
IMAGE_NAME="enterprise-api"
DOCKERFILE_PATH="."
BUILD_CONTEXT="."

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

# Function to get version from pom.xml
get_version() {
    if command -v mvn &> /dev/null; then
        mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "latest"
    else
        echo "latest"
    fi
}

# Function to get git commit hash
get_git_hash() {
    if command -v git &> /dev/null && git rev-parse --git-dir > /dev/null 2>&1; then
        git rev-parse --short HEAD 2>/dev/null || echo "unknown"
    else
        echo "unknown"
    fi
}

# Parse command line arguments
PUSH=false
NO_CACHE=false
VERSION=""
PLATFORM=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --push)
            PUSH=true
            shift
            ;;
        --no-cache)
            NO_CACHE=true
            shift
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --platform)
            PLATFORM="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --push          Push image to registry after build"
            echo "  --no-cache      Build without using cache"
            echo "  --version TAG   Specify version tag (default: from pom.xml)"
            echo "  --platform ARCH Specify target platform (e.g., linux/amd64,linux/arm64)"
            echo "  -h, --help      Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Set version if not provided
if [ -z "$VERSION" ]; then
    VERSION=$(get_version)
fi

GIT_HASH=$(get_git_hash)
BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

print_status "Building Docker image for Enterprise Spring Boot API"
print_status "Version: $VERSION"
print_status "Git Hash: $GIT_HASH"
print_status "Build Date: $BUILD_DATE"

# Build Docker build command
DOCKER_BUILD_CMD="docker build"

# Add platform if specified
if [ -n "$PLATFORM" ]; then
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD --platform $PLATFORM"
fi

# Add no-cache if specified
if [ "$NO_CACHE" = true ]; then
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD --no-cache"
fi

# Add build arguments
DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD \
    --build-arg VERSION=$VERSION \
    --build-arg GIT_HASH=$GIT_HASH \
    --build-arg BUILD_DATE=$BUILD_DATE"

# Add tags
DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD \
    -t $IMAGE_NAME:$VERSION \
    -t $IMAGE_NAME:latest"

# Add git hash tag if available
if [ "$GIT_HASH" != "unknown" ]; then
    DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD -t $IMAGE_NAME:$GIT_HASH"
fi

# Add dockerfile and context
DOCKER_BUILD_CMD="$DOCKER_BUILD_CMD -f $DOCKERFILE_PATH/Dockerfile $BUILD_CONTEXT"

print_status "Executing: $DOCKER_BUILD_CMD"

# Execute the build
if eval $DOCKER_BUILD_CMD; then
    print_status "Docker image built successfully!"
    
    # Show image information
    print_status "Image details:"
    docker images | grep $IMAGE_NAME | head -3
    
    # Show image size
    IMAGE_SIZE=$(docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep "$IMAGE_NAME.*$VERSION" | awk '{print $3}')
    print_status "Image size: $IMAGE_SIZE"
    
    # Push if requested
    if [ "$PUSH" = true ]; then
        print_status "Pushing images to registry..."
        docker push $IMAGE_NAME:$VERSION
        docker push $IMAGE_NAME:latest
        if [ "$GIT_HASH" != "unknown" ]; then
            docker push $IMAGE_NAME:$GIT_HASH
        fi
        print_status "Images pushed successfully!"
    fi
    
    print_status "Build completed successfully!"
else
    print_error "Docker build failed!"
    exit 1
fi