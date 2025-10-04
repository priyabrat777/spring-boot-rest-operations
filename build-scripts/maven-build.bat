@echo off
REM Enterprise Spring Boot API - Maven Build Script (Windows)
REM This script demonstrates various Maven build operations and profiles

setlocal enabledelayedexpansion

REM Function to print status messages
:print_status
echo [INFO] %~1
goto :eof

:print_success
echo [SUCCESS] %~1
goto :eof

:print_error
echo [ERROR] %~1
goto :eof

REM Function to run Maven command with error handling
:run_maven
set "command=%~1"
set "description=%~2"

call :print_status "Running: %description%"
echo Command: mvn %command%

mvn %command%
if errorlevel 1 (
    call :print_error "%description% failed"
    exit /b 1
)
call :print_success "%description% completed successfully"
echo.
goto :eof

REM Main build functions
:clean_build
call :print_status "Starting clean build..."
call :run_maven "clean compile" "Clean and compile"
goto :eof

:run_tests
call :print_status "Running all tests..."
call :run_maven "test" "Unit tests"
call :run_maven "integration-test" "Integration tests"
goto :eof

:quality_check
call :print_status "Running quality checks..."
call :run_maven "verify -P quality" "Code quality analysis"
goto :eof

:generate_site
call :print_status "Generating project site..."
call :run_maven "site" "Site generation"
goto :eof

:package_application
call :print_status "Packaging application..."
call :run_maven "package -DskipTests" "Application packaging"
goto :eof

:dev_build
call :print_status "Development build..."
call :run_maven "clean compile test -P dev" "Development profile build"
goto :eof

:ci_build
call :print_status "CI/CD build..."
call :run_maven "clean verify -P ci" "CI profile build with quality gates"
goto :eof

:prod_build
call :print_status "Production build..."
call :run_maven "clean package -P prod -DskipTests" "Production profile build"
goto :eof

:fast_build
call :print_status "Fast build (no tests)..."
call :run_maven "clean package -P fast" "Fast build profile"
goto :eof

:dependency_check
call :print_status "Checking for dependency updates..."
call :run_maven "versions:display-dependency-updates" "Dependency updates check"
call :run_maven "versions:display-plugin-updates" "Plugin updates check"
goto :eof

:full_build
call :print_status "Starting full build pipeline..."
call :clean_build
call :run_tests
call :quality_check
call :package_application
call :generate_site
call :print_success "Full build pipeline completed successfully!"
goto :eof

:show_help
echo Enterprise Spring Boot API - Maven Build Script (Windows)
echo.
echo Usage: %0 [COMMAND]
echo.
echo Commands:
echo   clean-build     - Clean and compile the project
echo   test           - Run all tests (unit + integration)
echo   quality        - Run code quality checks
echo   site           - Generate project documentation site
echo   package        - Package the application
echo   dev            - Development profile build
echo   ci             - CI/CD profile build with quality gates
echo   prod           - Production profile build
echo   fast           - Fast build without tests
echo   deps           - Check for dependency updates
echo   full           - Full build with all checks
echo   help           - Show this help message
echo.
echo Examples:
echo   %0 dev         # Development build
echo   %0 ci          # CI build with quality gates
echo   %0 full        # Complete build with all checks
goto :eof

REM Main script logic
set "command=%~1"
if "%command%"=="" set "command=help"

if "%command%"=="clean-build" goto :clean_build
if "%command%"=="clean" goto :clean_build
if "%command%"=="test" goto :run_tests
if "%command%"=="quality" goto :quality_check
if "%command%"=="site" goto :generate_site
if "%command%"=="package" goto :package_application
if "%command%"=="dev" goto :dev_build
if "%command%"=="ci" goto :ci_build
if "%command%"=="prod" goto :prod_build
if "%command%"=="fast" goto :fast_build
if "%command%"=="deps" goto :dependency_check
if "%command%"=="full" goto :full_build
if "%command%"=="help" goto :show_help

goto :show_help