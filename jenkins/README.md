# Jenkins CI/CD Pipeline for Enterprise Spring Boot API

This directory contains the complete Jenkins CI/CD pipeline configuration for the Enterprise Spring Boot API project. The pipeline implements industry best practices for continuous integration, testing, quality analysis, and deployment.

## 📁 Directory Structure

```
jenkins/
├── Jenkinsfile                           # Main pipeline definition
├── docker-compose.yml                    # Docker Compose for local Jenkins setup
├── jenkins-casc.yaml                     # Jenkins Configuration as Code
├── pipeline-config.yaml                  # Pipeline configuration settings
├── pipeline-library.yaml                 # Shared library configuration
├── setup-jenkins.sh                      # Automated Jenkins setup script
├── job-dsl/
│   └── enterprise-api-pipeline.groovy    # Job DSL for creating pipeline jobs
├── shared-library/
│   └── vars/
│       └── enterprisePipeline.groovy     # Shared pipeline library functions
└── README.md                             # This file
```

## 🚀 Quick Start

### Option 1: Docker Compose Setup (Recommended for Local Development)

1. **Start the Jenkins environment:**
   ```bash
   cd jenkins
   docker-compose up -d
   ```

2. **Wait for services to start:**
   ```bash
   # Check service health
   docker-compose ps
   
   # View Jenkins logs
   docker-compose logs -f jenkins
   ```

3. **Access Jenkins:**
   - URL: http://localhost:8080
   - Username: `admin`
   - Password: `admin123`

4. **Access SonarQube:**
   - URL: http://localhost:9000
   - Username: `admin`
   - Password: `admin`

### Option 2: Manual Setup

1. **Set environment variables:**
   ```bash
   export JENKINS_URL="http://your-jenkins-server:8080"
   export JENKINS_USER="admin"
   export JENKINS_TOKEN="your-jenkins-api-token"
   export GITHUB_USERNAME="your-github-username"
   export GITHUB_TOKEN="your-github-token"
   export SONAR_TOKEN="your-sonarqube-token"
   ```

2. **Run the setup script:**
   ```bash
   chmod +x setup-jenkins.sh
   ./setup-jenkins.sh
   ```

## 🔧 Pipeline Features

### Core Capabilities

- **Multi-stage Pipeline**: Comprehensive CI/CD with build, test, quality analysis, and deployment stages
- **Parallel Execution**: Unit tests, integration tests, and security scans run in parallel for faster feedback
- **Quality Gates**: SonarQube integration with configurable quality thresholds
- **Security Scanning**: OWASP dependency check for vulnerability detection
- **Docker Support**: Container building and registry publishing
- **Multi-environment Deployment**: Staging and production deployment with approval gates

### Pipeline Stages

1. **Checkout**: Source code retrieval and build information setup
2. **Build Setup**: Environment preparation and validation
3. **Compile**: Java compilation with error handling
4. **Unit Tests**: JUnit test execution with JaCoCo coverage
5. **Integration Tests**: Failsafe integration test execution
6. **Code Quality Analysis**: 
   - SonarQube static analysis
   - OWASP security vulnerability scanning
7. **Quality Gate**: SonarQube quality gate validation
8. **Package Application**: Maven packaging and artifact creation
9. **Build Docker Image**: Container image creation and tagging
10. **Performance Tests**: Performance benchmark execution
11. **Create Release**: GitHub release creation with artifacts
12. **Deploy to Staging**: Automated staging deployment
13. **Smoke Tests**: Post-deployment validation
14. **Production Deployment Approval**: Manual approval gate
15. **Deploy to Production**: Production deployment

### Branch Strategy

- **Main Branch**: Full pipeline with production deployment
- **Develop Branch**: Full pipeline with staging deployment
- **Feature Branches**: Build and test only (via multi-branch pipeline)
- **Release Branches**: Full pipeline with release candidate handling

## 📊 Quality Gates and Thresholds

### Code Coverage
- **Minimum Coverage**: 80%
- **Fail Build**: Yes
- **Coverage Tool**: JaCoCo

### Security Scanning
- **High Vulnerabilities**: 0 allowed
- **Medium Vulnerabilities**: 5 allowed
- **Tool**: OWASP Dependency Check

### SonarQube Quality Gate
- **Quality Gate**: "Sonar way"
- **Fail on Quality Gate**: Yes
- **Timeout**: 5 minutes

## 🔐 Security Configuration

### Credentials Required

1. **github-credentials**: GitHub repository access
   - Type: Username/Password
   - Username: GitHub username
   - Password: GitHub personal access token

2. **sonar-token**: SonarQube authentication
   - Type: Secret Text
   - Secret: SonarQube user token

3. **docker-registry-credentials**: Docker registry access
   - Type: Username/Password
   - Username: Registry username
   - Password: Registry token/password

4. **slack-token**: Slack notifications (optional)
   - Type: Secret Text
   - Secret: Slack webhook URL or bot token

### Environment Variables

```bash
# Jenkins Configuration
JENKINS_URL=http://localhost:8080
JENKINS_USER=admin
JENKINS_TOKEN=your-api-token

# GitHub Integration
GITHUB_USERNAME=your-username
GITHUB_TOKEN=your-personal-access-token

# SonarQube Integration
SONAR_URL=http://localhost:9000
SONAR_TOKEN=your-sonar-token

# Docker Registry
DOCKER_USERNAME=your-docker-username
DOCKER_TOKEN=your-docker-token

# Email Configuration
SMTP_HOST=smtp.enterprise.com
SMTP_PORT=587
SMTP_USERNAME=jenkins@enterprise.com
SMTP_PASSWORD=your-smtp-password

# Slack Integration (Optional)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
SLACK_CHANNEL=#ci-cd
```

## 📋 Pipeline Jobs

### 1. Main CI/CD Pipeline
- **Job Name**: `enterprise-spring-boot-api`
- **Trigger**: Git push to main/develop branches
- **Schedule**: Daily at 2 AM (weekdays)
- **Features**: Full pipeline with deployment

### 2. Multi-branch Pipeline
- **Job Name**: `enterprise-spring-boot-api-multibranch`
- **Trigger**: All branches and pull requests
- **Features**: Build and test validation

### 3. Release Pipeline
- **Job Name**: `enterprise-spring-boot-api-release`
- **Trigger**: Manual
- **Features**: Maven release plugin integration

### 4. Nightly Build
- **Job Name**: `enterprise-spring-boot-api-nightly`
- **Trigger**: Daily at 1 AM
- **Features**: Extended testing and reporting

## 🔔 Notifications

### Email Notifications
- **Recipients**: Configurable via environment variables
- **Triggers**: Build failures, unstable builds
- **Format**: HTML with build details and links

### Slack Notifications
- **Channel**: Configurable (default: #ci-cd)
- **Triggers**: All build status changes
- **Format**: Rich messages with build status and links

## 📈 Monitoring and Reporting

### Build Artifacts
- JAR files with fingerprinting
- Test reports (JUnit XML)
- Coverage reports (JaCoCo)
- Security reports (OWASP HTML)
- Build logs and console output

### Health Checks
- Jenkins service health monitoring
- SonarQube connectivity validation
- Docker daemon availability
- Git repository accessibility

## 🛠️ Customization

### Pipeline Parameters
The pipeline supports several parameters for customization:

- `SKIP_TESTS`: Skip test execution (boolean)
- `DEPLOY_ENVIRONMENT`: Target deployment environment (choice)
- `FORCE_DEPLOY`: Force deployment despite quality gate failures (boolean)
- `MAVEN_GOALS`: Custom Maven goals (string)
- `DOCKER_TAG`: Custom Docker tag (string)

### Configuration Files
- **pipeline-config.yaml**: Pipeline-specific settings
- **jenkins-casc.yaml**: Jenkins system configuration
- **pipeline-library.yaml**: Shared library configuration

## 🔍 Troubleshooting

### Common Issues

1. **Plugin Installation Failures**
   ```bash
   # Check Jenkins logs
   docker-compose logs jenkins
   
   # Restart Jenkins
   docker-compose restart jenkins
   ```

2. **SonarQube Connection Issues**
   ```bash
   # Check SonarQube health
   curl http://localhost:9000/api/system/status
   
   # Verify credentials in Jenkins
   ```

3. **Docker Build Failures**
   ```bash
   # Check Docker daemon
   docker info
   
   # Verify Docker socket mount
   docker-compose exec jenkins ls -la /var/run/docker.sock
   ```

4. **Git Authentication Issues**
   ```bash
   # Test Git credentials
   git ls-remote https://github.com/enterprise/spring-boot-api.git
   
   # Update credentials in Jenkins
   ```

### Debug Mode
Enable debug logging by setting environment variables:

```bash
export JENKINS_OPTS="--httpPort=8080 --loggerLevel=ALL"
export JAVA_OPTS="-Xmx2048m -Djava.util.logging.config.file=/var/jenkins_home/log.properties"
```

## 📚 Additional Resources

- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Jenkins Configuration as Code](https://github.com/jenkinsci/configuration-as-code-plugin)
- [SonarQube Integration](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-jenkins/)
- [Docker Pipeline Plugin](https://plugins.jenkins.io/docker-workflow/)
- [GitHub Branch Source Plugin](https://plugins.jenkins.io/github-branch-source/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test the pipeline configuration
5. Submit a pull request

## 📄 License

This Jenkins configuration is part of the Enterprise Spring Boot API project and follows the same licensing terms.

---

For questions or support, please contact the DevOps team at devops@enterprise.com.