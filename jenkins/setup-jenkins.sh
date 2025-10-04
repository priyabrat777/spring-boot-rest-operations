#!/bin/bash

# Jenkins Setup Script for Enterprise Spring Boot API
# This script sets up Jenkins with all required plugins and configurations

set -e

echo "🚀 Setting up Jenkins for Enterprise Spring Boot API..."

# Jenkins configuration
JENKINS_URL="${JENKINS_URL:-http://localhost:8080}"
JENKINS_USER="${JENKINS_USER:-admin}"
JENKINS_TOKEN="${JENKINS_TOKEN}"

if [ -z "$JENKINS_TOKEN" ]; then
    echo "❌ JENKINS_TOKEN environment variable is required"
    exit 1
fi

# Function to install Jenkins plugin
install_plugin() {
    local plugin_name=$1
    echo "📦 Installing plugin: $plugin_name"
    
    curl -X POST \
        -u "$JENKINS_USER:$JENKINS_TOKEN" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "plugin=$plugin_name" \
        "$JENKINS_URL/pluginManager/installNecessaryPlugins"
}

# Function to create Jenkins credential
create_credential() {
    local credential_id=$1
    local credential_type=$2
    local credential_data=$3
    
    echo "🔐 Creating credential: $credential_id"
    
    curl -X POST \
        -u "$JENKINS_USER:$JENKINS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$credential_data" \
        "$JENKINS_URL/credentials/store/system/domain/_/createCredentials"
}

# Function to create Jenkins job
create_job() {
    local job_name=$1
    local job_config=$2
    
    echo "📋 Creating job: $job_name"
    
    curl -X POST \
        -u "$JENKINS_USER:$JENKINS_TOKEN" \
        -H "Content-Type: application/xml" \
        -d "$job_config" \
        "$JENKINS_URL/createItem?name=$job_name"
}

echo "📦 Installing required Jenkins plugins..."

# Core plugins
install_plugin "workflow-aggregator"
install_plugin "pipeline-stage-view"
install_plugin "pipeline-graph-analysis"
install_plugin "pipeline-rest-api"

# SCM plugins
install_plugin "git"
install_plugin "github"
install_plugin "github-branch-source"
install_plugin "multibranch-scan-webhook-trigger"

# Build tools
install_plugin "maven-plugin"
install_plugin "pipeline-maven"

# Testing and quality
install_plugin "junit"
install_plugin "jacoco"
install_plugin "sonar"
install_plugin "owasp-dependency-check"
install_plugin "htmlpublisher"

# Docker
install_plugin "docker-plugin"
install_plugin "docker-workflow"

# Notifications
install_plugin "email-ext"
install_plugin "slack"
install_plugin "build-user-vars-plugin"

# Utilities
install_plugin "build-timeout"
install_plugin "timestamper"
install_plugin "ws-cleanup"
install_plugin "build-name-setter"
install_plugin "description-setter"

# Job DSL
install_plugin "job-dsl"
install_plugin "configuration-as-code"

echo "⏳ Waiting for Jenkins to restart after plugin installation..."
sleep 30

echo "🔐 Creating Jenkins credentials..."

# GitHub credentials
github_credential='{
    "": "0",
    "credentials": {
        "scope": "GLOBAL",
        "id": "github-credentials",
        "username": "'${GITHUB_USERNAME:-admin}'",
        "password": "'${GITHUB_TOKEN:-changeme}'",
        "description": "GitHub credentials for repository access",
        "$class": "com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl"
    }
}'

create_credential "github-credentials" "username-password" "$github_credential"

# SonarQube token
sonar_credential='{
    "": "0",
    "credentials": {
        "scope": "GLOBAL",
        "id": "sonar-token",
        "secret": "'${SONAR_TOKEN:-changeme}'",
        "description": "SonarQube authentication token",
        "$class": "org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl"
    }
}'

create_credential "sonar-token" "secret-text" "$sonar_credential"

# Docker registry credentials
docker_credential='{
    "": "0",
    "credentials": {
        "scope": "GLOBAL",
        "id": "docker-registry-credentials",
        "username": "'${DOCKER_USERNAME:-admin}'",
        "password": "'${DOCKER_TOKEN:-changeme}'",
        "description": "Docker registry credentials",
        "$class": "com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl"
    }
}'

create_credential "docker-registry-credentials" "username-password" "$docker_credential"

echo "🛠️ Configuring Jenkins global settings..."

# Configure Maven
maven_config='{
    "name": "Maven-3.9.0",
    "home": "/opt/maven",
    "properties": []
}'

curl -X POST \
    -u "$JENKINS_USER:$JENKINS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$maven_config" \
    "$JENKINS_URL/configure"

# Configure JDK
jdk_config='{
    "name": "JDK-21",
    "home": "/usr/lib/jvm/java-21-openjdk",
    "properties": []
}'

curl -X POST \
    -u "$JENKINS_USER:$JENKINS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$jdk_config" \
    "$JENKINS_URL/configure"

echo "📋 Creating Jenkins jobs using Job DSL..."

# Create seed job for Job DSL
seed_job_config='<?xml version="1.0" encoding="UTF-8"?>
<project>
    <actions/>
    <description>Seed job for creating Enterprise Spring Boot API pipeline jobs</description>
    <keepDependencies>false</keepDependencies>
    <properties/>
    <scm class="hudson.plugins.git.GitSCM">
        <configVersion>2</configVersion>
        <userRemoteConfigs>
            <hudson.plugins.git.UserRemoteConfig>
                <url>https://github.com/enterprise/spring-boot-api.git</url>
                <credentialsId>github-credentials</credentialsId>
            </hudson.plugins.git.UserRemoteConfig>
        </userRemoteConfigs>
        <branches>
            <hudson.plugins.git.BranchSpec>
                <name>*/main</name>
            </hudson.plugins.git.BranchSpec>
        </branches>
    </scm>
    <canRoam>true</canRoam>
    <disabled>false</disabled>
    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
    <triggers/>
    <concurrentBuild>false</concurrentBuild>
    <builders>
        <javaposse.jobdsl.plugin.ExecuteDslScripts>
            <targets>jenkins/job-dsl/*.groovy</targets>
            <usingScriptText>false</usingScriptText>
            <sandbox>false</sandbox>
            <ignoreExisting>false</ignoreExisting>
            <ignoreMissingFiles>false</ignoreMissingFiles>
            <failOnMissingPlugin>false</failOnMissingPlugin>
            <unstableOnDeprecation>false</unstableOnDeprecation>
            <removedJobAction>IGNORE</removedJobAction>
            <removedViewAction>IGNORE</removedViewAction>
            <lookupStrategy>JENKINS_ROOT</lookupStrategy>
        </javaposse.jobdsl.plugin.ExecuteDslScripts>
    </builders>
    <publishers/>
    <buildWrappers/>
</project>'

create_job "enterprise-api-seed-job" "$seed_job_config"

echo "🔧 Configuring Jenkins system settings..."

# Configure email
email_config='{
    "smtpServer": "smtp.enterprise.com",
    "smtpPort": "587",
    "useSsl": true,
    "smtpAuthUsername": "'${SMTP_USERNAME:-jenkins@enterprise.com}'",
    "smtpAuthPassword": "'${SMTP_PASSWORD:-changeme}'",
    "adminAddress": "jenkins@enterprise.com"
}'

curl -X POST \
    -u "$JENKINS_USER:$JENKINS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$email_config" \
    "$JENKINS_URL/configure"

# Configure SonarQube server
sonar_config='{
    "name": "SonarQube",
    "serverUrl": "'${SONAR_URL:-http://localhost:9000}'",
    "credentialsId": "sonar-token"
}'

curl -X POST \
    -u "$JENKINS_USER:$JENKINS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$sonar_config" \
    "$JENKINS_URL/configure"

echo "🚀 Running seed job to create pipeline jobs..."

# Trigger seed job
curl -X POST \
    -u "$JENKINS_USER:$JENKINS_TOKEN" \
    "$JENKINS_URL/job/enterprise-api-seed-job/build"

echo "⏳ Waiting for seed job to complete..."
sleep 10

echo "✅ Jenkins setup completed successfully!"
echo ""
echo "📋 Created Jobs:"
echo "  - enterprise-spring-boot-api (Main CI/CD Pipeline)"
echo "  - enterprise-spring-boot-api-multibranch (Multi-branch Pipeline)"
echo "  - enterprise-spring-boot-api-release (Release Pipeline)"
echo "  - enterprise-spring-boot-api-nightly (Nightly Build)"
echo ""
echo "🔗 Jenkins URL: $JENKINS_URL"
echo "👤 Username: $JENKINS_USER"
echo ""
echo "🎉 You can now start using the Jenkins pipelines!"

# Create a summary report
cat > jenkins-setup-report.md << EOF
# Jenkins Setup Report

## Overview
Jenkins has been successfully configured for the Enterprise Spring Boot API project.

## Installed Plugins
- Pipeline plugins (workflow-aggregator, pipeline-stage-view, etc.)
- SCM plugins (git, github, github-branch-source)
- Build tools (maven-plugin, pipeline-maven)
- Testing and quality (junit, jacoco, sonar, owasp-dependency-check)
- Docker support (docker-plugin, docker-workflow)
- Notifications (email-ext, slack)
- Job DSL and Configuration as Code

## Created Credentials
- \`github-credentials\`: GitHub repository access
- \`sonar-token\`: SonarQube authentication
- \`docker-registry-credentials\`: Docker registry access

## Created Jobs
1. **enterprise-spring-boot-api**: Main CI/CD pipeline
2. **enterprise-spring-boot-api-multibranch**: Multi-branch pipeline for feature branches
3. **enterprise-spring-boot-api-release**: Release management pipeline
4. **enterprise-spring-boot-api-nightly**: Nightly builds with extended testing

## Global Configuration
- Maven 3.9.0 configured
- JDK 21 configured
- Email server configured
- SonarQube server configured

## Next Steps
1. Verify all jobs are created successfully
2. Test the main pipeline with a sample build
3. Configure webhook triggers in GitHub
4. Set up Slack notifications if needed
5. Review and adjust quality gates as needed

## Access Information
- Jenkins URL: $JENKINS_URL
- Username: $JENKINS_USER
- All jobs are located in the 'Enterprise Spring Boot API' folder

EOF

echo "📄 Setup report saved to jenkins-setup-report.md"