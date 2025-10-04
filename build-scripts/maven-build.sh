#!/bin/bash

# Enterprise Spring Boot API - Maven Build Script
# This script demonstrates various Maven build operations and profiles

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to run Maven command with error handling
run_maven() {
    local command="$1"
    local description="$2"
    
    print_status "Running: $description"
    echo "Command: mvn $command"
    
    if mvn $command; then
        print_success "$description completed successfully"
    else
        print_error "$description failed"
        exit 1
    fi
    echo ""
}

# Main build functions
clean_build() {
    print_status "Starting clean build..."
    run_maven "clean compile" "Clean and compile"
}

run_tests() {
    print_status "Running all tests..."
    run_maven "test" "Unit tests"
    run_maven "integration-test" "Integration tests"
}

quality_check() {
    print_status "Running quality checks..."
    run_maven "verify -P quality" "Code quality analysis"
}

generate_site() {
    print_status "Generating project site..."
    run_maven "site" "Site generation"
}

package_application() {
    print_status "Packaging application..."
    run_maven "package -DskipTests" "Application packaging"
}

release_prepare() {
    print_status "Preparing release..."
    run_maven "release:prepare -DdryRun=true" "Release preparation (dry run)"
}

release_perform() {
    print_status "Performing release..."
    run_maven "release:perform" "Release execution"
}

# Profile-specific builds
dev_build() {
    print_status "Development build..."
    run_maven "clean compile test -P dev" "Development profile build"
}

ci_build() {
    print_status "CI/CD build..."
    run_maven "clean verify -P ci" "CI profile build with quality gates"
}

prod_build() {
    print_status "Production build..."
    run_maven "clean package -P prod -DskipTests" "Production profile build"
}

fast_build() {
    print_status "Fast build (no tests)..."
    run_maven "clean package -P fast" "Fast build profile"
}

# Utility functions
dependency_check() {
    print_status "Checking for dependency updates..."
    run_maven "versions:display-dependency-updates" "Dependency updates check"
    run_maven "versions:display-plugin-updates" "Plugin updates check"
}

security_check() {
    print_status "Running security checks..."
    # Note: This would require OWASP dependency check plugin
    print_warning "Security check plugin not configured. Add OWASP dependency-check-maven plugin for security scanning."
}

# Help function
show_help() {
    echo "Enterprise Spring Boot API - Maven Build Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  clean-build     - Clean and compile the project"
    echo "  test           - Run all tests (unit + integration)"
    echo "  quality        - Run code quality checks"
    echo "  site           - Generate project documentation site"
    echo "  package        - Package the application"
    echo "  dev            - Development profile build"
    echo "  ci             - CI/CD profile build with quality gates"
    echo "  prod           - Production profile build"
    echo "  fast           - Fast build without tests"
    echo "  deps           - Check for dependency updates"
    echo "  security       - Run security checks"
    echo "  release-prep   - Prepare release (dry run)"
    echo "  release        - Perform release"
    echo "  full           - Full build with all checks"
    echo "  help           - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 dev         # Development build"
    echo "  $0 ci          # CI build with quality gates"
    echo "  $0 full        # Complete build with all checks"
}

# Full build pipeline
full_build() {
    print_status "Starting full build pipeline..."
    clean_build
    run_tests
    quality_check
    package_application
    generate_site
    print_success "Full build pipeline completed successfully!"
}

# Main script logic
case "${1:-help}" in
    "clean-build"|"clean")
        clean_build
        ;;
    "test")
        run_tests
        ;;
    "quality")
        quality_check
        ;;
    "site")
        generate_site
        ;;
    "package")
        package_application
        ;;
    "dev")
        dev_build
        ;;
    "ci")
        ci_build
        ;;
    "prod")
        prod_build
        ;;
    "fast")
        fast_build
        ;;
    "deps")
        dependency_check
        ;;
    "security")
        security_check
        ;;
    "release-prep")
        release_prepare
        ;;
    "release")
        release_perform
        ;;
    "full")
        full_build
        ;;
    "help"|*)
        show_help
        ;;
esac