# Maven Build and Release Management

This directory contains comprehensive Maven build and release management configuration for the Enterprise Spring Boot API project.

## Overview

The Maven configuration includes:

- **Complete POM Configuration**: Enterprise-level Maven POM with all required dependencies and plugins
- **Maven Release Plugin**: Automated version management and release processes
- **Maven Profiles**: Different build profiles for various environments (dev, test, prod, ci, release)
- **Surefire & Failsafe Plugins**: Comprehensive unit and integration test configuration
- **Maven Site Generation**: Automated documentation and reporting
- **Build Scripts**: Cross-platform build automation scripts

## Maven Profiles

### Development Profile (`dev`)
- **Activation**: Active by default
- **Purpose**: Local development with relaxed quality gates
- **Features**:
  - Lower test coverage thresholds (80% line, 75% branch)
  - Code analysis disabled by default
  - Integration tests enabled

```bash
mvn clean compile -P dev
```

### Test Profile (`test`)
- **Purpose**: Testing environment configuration
- **Features**:
  - Full test execution
  - Code analysis enabled
  - Standard coverage thresholds

```bash
mvn clean verify -P test
```

### Production Profile (`prod`)
- **Purpose**: Production build configuration
- **Features**:
  - Integration tests skipped
  - Code analysis disabled
  - Optimized for deployment

```bash
mvn clean package -P prod
```

### CI Profile (`ci`)
- **Purpose**: Continuous Integration with strict quality gates
- **Features**:
  - High coverage thresholds (95% line, 90% branch)
  - All code quality checks enabled
  - Checkstyle, PMD, and SpotBugs validation

```bash
mvn clean verify -P ci
```

### Release Profile (`release`)
- **Purpose**: Release preparation and artifact generation
- **Features**:
  - Source and Javadoc JAR generation
  - Assembly plugin for distribution packages
  - Full documentation generation

```bash
mvn clean deploy -P release
```

### Fast Profile (`fast`)
- **Purpose**: Quick builds without tests and analysis
- **Features**:
  - All tests skipped
  - Code analysis disabled
  - Documentation generation skipped

```bash
mvn clean package -P fast
```

### Quality Profile (`quality`)
- **Purpose**: Code quality analysis only
- **Features**:
  - Checkstyle validation
  - PMD analysis
  - SpotBugs detection

```bash
mvn verify -P quality
```

## Maven Plugins Configuration

### Core Build Plugins

#### Maven Compiler Plugin
- **Java Version**: 21 with preview features enabled
- **Encoding**: UTF-8
- **Compiler Args**: Lint warnings, parameter names, error on warnings
- **Memory**: 1GB max heap for compilation

#### Maven Surefire Plugin (Unit Tests)
- **Parallel Execution**: Classes level with 4 threads
- **Memory**: 2GB max heap with G1GC
- **Includes**: `**/*Test.java`, `**/*Tests.java`, `**/*TestCase.java`
- **Excludes**: Integration test patterns
- **Reports**: XML format with detailed test information

#### Maven Failsafe Plugin (Integration Tests)
- **Memory**: 4GB max heap for integration tests
- **Includes**: `**/*IntegrationTest.java`, `**/*IT.java`, `**/*ITCase.java`
- **TestContainers**: Reuse enabled for faster execution
- **Fork**: Single fork per test class

#### Maven Release Plugin
- **Tag Format**: `v{version}` (e.g., v1.0.0)
- **Goals**: `deploy site:site site:deploy`
- **SCM**: Git with SSH authentication
- **Preparation**: `clean verify`
- **Push Changes**: Disabled (manual control)

### Code Quality Plugins

#### JaCoCo Maven Plugin
- **Coverage Thresholds**: Configurable per profile
- **Reports**: Unit, integration, and aggregate coverage
- **Exclusions**: Configuration classes, DTOs, entities, generated code

#### Checkstyle Plugin
- **Configuration**: Google Java Style Guide
- **Execution**: Validate phase in CI profile
- **Fail on Error**: Enabled in CI builds

#### PMD Plugin
- **Rules**: Default PMD ruleset
- **Execution**: Validate phase in quality profiles
- **Reports**: XML and HTML formats

#### SpotBugs Plugin
- **Effort**: Maximum
- **Threshold**: Low (catch all potential issues)
- **Reports**: XML format for CI integration

### Reporting Plugins

#### Maven Site Plugin
- **Skin**: Maven Fluido Skin
- **Encoding**: UTF-8
- **Reports**: Comprehensive project information
- **Sitemap**: Auto-generated

#### Maven Project Info Reports Plugin
- **Reports**: Dependencies, plugins, team, licenses, SCM, CI management
- **Dependency Details**: Enabled with location information

#### Versions Maven Plugin
- **Reports**: Dependency updates, plugin updates, property updates
- **Backup POMs**: Disabled for cleaner workspace

## Build Scripts

### Unix/Linux/macOS (`maven-build.sh`)

```bash
# Make executable
chmod +x build-scripts/maven-build.sh

# Development build
./build-scripts/maven-build.sh dev

# CI build with quality gates
./build-scripts/maven-build.sh ci

# Full build pipeline
./build-scripts/maven-build.sh full

# Show help
./build-scripts/maven-build.sh help
```

### Windows (`maven-build.bat`)

```cmd
REM Development build
build-scripts\maven-build.bat dev

REM CI build with quality gates
build-scripts\maven-build.bat ci

REM Full build pipeline
build-scripts\maven-build.bat full

REM Show help
build-scripts\maven-build.bat help
```

## Maven Configuration Files

### `.mvn/maven.config`
Default Maven command-line options:
- Batch mode for CI/CD
- UTF-8 encoding
- Development profile activation
- Parallel builds (1 thread per CPU core)
- Fail-fast on errors

### `.mvn/jvm.config`
JVM configuration for Maven process:
- Memory settings (512MB-2GB heap)
- G1 garbage collector
- Performance optimizations
- UTF-8 encoding
- UTC timezone

### `.mvn/wrapper/`
Maven Wrapper configuration:
- Maven version: 3.9.9
- Automatic download and installation
- Consistent builds across environments

## Release Management

### Preparing a Release

1. **Ensure Clean Working Directory**
   ```bash
   git status
   git stash  # if needed
   ```

2. **Run Release Preparation (Dry Run)**
   ```bash
   mvn release:prepare -DdryRun=true
   ```

3. **Run Actual Release Preparation**
   ```bash
   mvn release:prepare
   ```

4. **Perform Release**
   ```bash
   mvn release:perform
   ```

### Release Configuration

The release process:
1. Validates no uncommitted changes
2. Runs full test suite
3. Updates version numbers
4. Creates Git tag
5. Builds and deploys artifacts
6. Generates and deploys site documentation
7. Updates to next development version

### Version Management

- **Development**: `X.Y.Z-SNAPSHOT`
- **Release**: `X.Y.Z`
- **Tag Format**: `vX.Y.Z`
- **Next Development**: `X.Y.(Z+1)-SNAPSHOT`

## Site Generation

### Generate Project Site

```bash
# Generate site with reports
mvn site

# Generate and deploy site
mvn site:site site:deploy
```

### Site Content

The generated site includes:
- Project information and summary
- API documentation (Javadoc)
- Test coverage reports (JaCoCo)
- Test execution reports (Surefire)
- Dependency analysis
- Plugin information
- Code quality reports
- Version update reports

### Site Location

- **Local**: `target/site/index.html`
- **Deployed**: Configured in `distributionManagement/site`

## Quality Gates

### Coverage Thresholds

| Profile | Line Coverage | Branch Coverage |
|---------|---------------|-----------------|
| dev     | 80%           | 75%             |
| test    | 90%           | 85%             |
| ci      | 95%           | 90%             |

### Code Quality Checks

- **Checkstyle**: Google Java Style Guide compliance
- **PMD**: Code quality and best practices
- **SpotBugs**: Bug pattern detection
- **Dependency Check**: Security vulnerability scanning (when configured)

## Troubleshooting

### Common Issues

1. **Memory Issues**
   - Increase heap size in `.mvn/jvm.config`
   - Use `-Xmx4g` for large projects

2. **Test Failures**
   - Run tests individually: `mvn test -Dtest=ClassName`
   - Skip tests temporarily: `mvn package -DskipTests`

3. **Plugin Version Conflicts**
   - Use `mvn dependency:tree` to analyze
   - Update plugin versions in `pluginManagement`

4. **Site Generation Issues**
   - Clear target directory: `mvn clean`
   - Check site.xml configuration
   - Verify skin dependencies

### Getting Help

```bash
# Maven help
mvn help:help

# Plugin help
mvn <plugin>:help

# Effective POM
mvn help:effective-pom

# Active profiles
mvn help:active-profiles

# System information
mvn help:system
```

## Best Practices

1. **Always use profiles** for different environments
2. **Run quality checks** before committing code
3. **Keep dependencies updated** using versions plugin
4. **Use Maven wrapper** for consistent builds
5. **Document custom configurations** in team wikis
6. **Automate releases** using CI/CD pipelines
7. **Monitor build performance** and optimize as needed
8. **Backup release artifacts** and documentation

## Integration with CI/CD

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh './mvnw clean compile -P ci'
            }
        }
        
        stage('Test') {
            steps {
                sh './mvnw test -P ci'
            }
            post {
                always {
                    publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                    publishCoverage adapters: [jacocoAdapter('target/site/jacoco/jacoco.xml')]
                }
            }
        }
        
        stage('Quality') {
            steps {
                sh './mvnw verify -P ci'
            }
        }
        
        stage('Package') {
            steps {
                sh './mvnw package -P prod'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
        
        stage('Site') {
            steps {
                sh './mvnw site'
            }
            post {
                always {
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site',
                        reportFiles: 'index.html',
                        reportName: 'Project Site'
                    ])
                }
            }
        }
    }
}
```

This comprehensive Maven configuration provides enterprise-level build and release management capabilities for the Spring Boot API project.