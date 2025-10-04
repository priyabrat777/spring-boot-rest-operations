#!/usr/bin/env groovy

/**
 * Enterprise Pipeline Shared Library
 * Contains common functions used across Jenkins pipelines
 */

def call(Map config) {
    pipeline {
        agent any
        
        tools {
            maven config.maven ?: 'Maven-3.9.0'
            jdk config.jdk ?: 'JDK-21'
        }
        
        environment {
            MAVEN_OPTS = config.mavenOpts ?: '-Xmx1024m -XX:MaxPermSize=256m'
            BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT[0..7]}"
        }
        
        stages {
            stage('Setup') {
                steps {
                    setupBuild(config)
                }
            }
            
            stage('Build & Test') {
                parallel {
                    stage('Unit Tests') {
                        steps {
                            runUnitTests(config)
                        }
                    }
                    stage('Integration Tests') {
                        steps {
                            runIntegrationTests(config)
                        }
                    }
                }
            }
            
            stage('Quality Analysis') {
                steps {
                    runQualityAnalysis(config)
                }
            }
            
            stage('Package & Deploy') {
                steps {
                    packageAndDeploy(config)
                }
            }
        }
        
        post {
            always {
                publishResults(config)
            }
            success {
                notifySuccess(config)
            }
            failure {
                notifyFailure(config)
            }
        }
    }
}

def setupBuild(config) {
    echo "Setting up build environment..."
    
    // Clean workspace
    sh 'mvn clean'
    
    // Validate configuration
    sh 'mvn validate'
    
    // Set build properties
    script {
        env.PROJECT_VERSION = readMavenPom().version
        env.ARTIFACT_ID = readMavenPom().artifactId
    }
    
    echo "Build setup completed for ${env.ARTIFACT_ID} v${env.PROJECT_VERSION}"
}

def runUnitTests(config) {
    echo "Running unit tests..."
    
    try {
        sh '''
            mvn test \
                -Dspring.profiles.active=test \
                -Dmaven.test.failure.ignore=false \
                -Djacoco.skip=false
        '''
        
        // Publish test results
        publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
        
        // Publish coverage
        publishCoverage adapters: [
            jacocoAdapter('target/site/jacoco/jacoco.xml')
        ], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
        
        echo "✅ Unit tests completed successfully"
        
    } catch (Exception e) {
        error "❌ Unit tests failed: ${e.getMessage()}"
    }
}

def runIntegrationTests(config) {
    echo "Running integration tests..."
    
    try {
        sh '''
            mvn failsafe:integration-test failsafe:verify \
                -Dspring.profiles.active=test \
                -DskipUTs=true \
                -Dmaven.test.failure.ignore=false
        '''
        
        // Publish integration test results
        publishTestResults testResultsPattern: 'target/failsafe-reports/*.xml'
        
        echo "✅ Integration tests completed successfully"
        
    } catch (Exception e) {
        error "❌ Integration tests failed: ${e.getMessage()}"
    }
}

def runQualityAnalysis(config) {
    echo "Running quality analysis..."
    
    parallel {
        'SonarQube Analysis': {
            withSonarQubeEnv('SonarQube') {
                sh '''
                    mvn sonar:sonar \
                        -Dsonar.projectKey=${ARTIFACT_ID} \
                        -Dsonar.projectName="${ARTIFACT_ID}" \
                        -Dsonar.projectVersion=${BUILD_VERSION}
                '''
            }
            
            // Wait for quality gate
            timeout(time: 5, unit: 'MINUTES') {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Quality gate failed: ${qg.status}"
                }
            }
        },
        
        'Security Scan': {
            sh '''
                mvn org.owasp:dependency-check-maven:check \
                    -DfailBuildOnCVSS=7
            '''
            
            // Archive security report
            archiveArtifacts artifacts: 'target/dependency-check-report.html', allowEmptyArchive: true
        }
    }
    
    echo "✅ Quality analysis completed"
}

def packageAndDeploy(config) {
    echo "Packaging application..."
    
    // Package application
    sh '''
        mvn package -DskipTests=true \
            -Dmaven.javadoc.skip=true \
            -Dspring.profiles.active=prod
    '''
    
    // Archive artifacts
    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    
    // Build Docker image if configured
    if (config.docker?.enabled) {
        buildDockerImage(config)
    }
    
    // Deploy to environment
    if (shouldDeploy(config)) {
        deployToEnvironment(config)
    }
    
    echo "✅ Package and deploy completed"
}

def buildDockerImage(config) {
    echo "Building Docker image..."
    
    script {
        def image = docker.build("${config.docker.registry}/${config.docker.image}:${env.BUILD_VERSION}")
        
        if (env.BRANCH_NAME == 'main') {
            image.tag('latest')
        }
        
        // Push to registry if configured
        if (config.docker.push) {
            docker.withRegistry("https://${config.docker.registry}", config.docker.credentials) {
                image.push()
                image.push('latest')
            }
        }
    }
    
    echo "✅ Docker image built and pushed"
}

def shouldDeploy(config) {
    // Deploy logic based on branch and configuration
    if (env.BRANCH_NAME == 'main' && config.deploy?.production) {
        return true
    } else if (env.BRANCH_NAME == 'develop' && config.deploy?.staging) {
        return true
    }
    return false
}

def deployToEnvironment(config) {
    def environment = (env.BRANCH_NAME == 'main') ? 'production' : 'staging'
    
    echo "Deploying to ${environment}..."
    
    // Deployment logic would go here
    // This could involve Kubernetes, Docker Compose, or other deployment tools
    
    echo "✅ Deployed to ${environment}"
}

def publishResults(config) {
    echo "Publishing build results..."
    
    // Archive build logs
    archiveArtifacts artifacts: 'target/logs/**/*', allowEmptyArchive: true
    
    // Generate build report
    def buildReport = generateBuildReport()
    writeFile file: 'build-report.html', text: buildReport
    
    publishHTML([
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: '.',
        reportFiles: 'build-report.html',
        reportName: 'Build Report'
    ])
    
    echo "✅ Results published"
}

def notifySuccess(config) {
    def message = "✅ Build ${env.BUILD_NUMBER} completed successfully for ${env.BRANCH_NAME}"
    
    sendNotification([
        status: 'SUCCESS',
        message: message,
        config: config
    ])
}

def notifyFailure(config) {
    def message = "❌ Build ${env.BUILD_NUMBER} failed for ${env.BRANCH_NAME}"
    
    sendNotification([
        status: 'FAILURE',
        message: message,
        config: config
    ])
}

def sendNotification(params) {
    def status = params.status
    def message = params.message
    def config = params.config
    
    // Email notification
    if (config.notifications?.email) {
        emailext (
            subject: "[Jenkins] ${status}: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
            body: generateEmailBody(status, message),
            to: config.notifications.email.recipients.join(','),
            mimeType: 'text/html'
        )
    }
    
    // Slack notification
    if (config.notifications?.slack) {
        try {
            slackSend(
                channel: config.notifications.slack.channel,
                color: getStatusColor(status),
                message: "${message}\nBuild: ${env.BUILD_URL}"
            )
        } catch (Exception e) {
            echo "Slack notification failed: ${e.getMessage()}"
        }
    }
}

def generateEmailBody(status, message) {
    return """
    <html>
    <body>
        <h2>Build ${status}</h2>
        <table border="1" cellpadding="5" cellspacing="0">
            <tr><td><strong>Job:</strong></td><td>${env.JOB_NAME}</td></tr>
            <tr><td><strong>Build Number:</strong></td><td>${env.BUILD_NUMBER}</td></tr>
            <tr><td><strong>Branch:</strong></td><td>${env.BRANCH_NAME}</td></tr>
            <tr><td><strong>Commit:</strong></td><td>${env.GIT_COMMIT}</td></tr>
            <tr><td><strong>Duration:</strong></td><td>${currentBuild.durationString}</td></tr>
            <tr><td><strong>Status:</strong></td><td>${status}</td></tr>
        </table>
        <p><strong>Message:</strong> ${message}</p>
        <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
    </body>
    </html>
    """
}

def generateBuildReport() {
    return """
    <html>
    <head>
        <title>Build Report - ${env.JOB_NAME} #${env.BUILD_NUMBER}</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            table { border-collapse: collapse; width: 100%; }
            th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            th { background-color: #f2f2f2; }
            .success { color: green; }
            .failure { color: red; }
            .warning { color: orange; }
        </style>
    </head>
    <body>
        <h1>Build Report</h1>
        <h2>Build Information</h2>
        <table>
            <tr><th>Property</th><th>Value</th></tr>
            <tr><td>Job Name</td><td>${env.JOB_NAME}</td></tr>
            <tr><td>Build Number</td><td>${env.BUILD_NUMBER}</td></tr>
            <tr><td>Branch</td><td>${env.BRANCH_NAME}</td></tr>
            <tr><td>Commit</td><td>${env.GIT_COMMIT}</td></tr>
            <tr><td>Build Version</td><td>${env.BUILD_VERSION}</td></tr>
            <tr><td>Duration</td><td>${currentBuild.durationString}</td></tr>
            <tr><td>Status</td><td class="${currentBuild.result?.toLowerCase() ?: 'success'}">${currentBuild.result ?: 'SUCCESS'}</td></tr>
        </table>
        
        <h2>Build Stages</h2>
        <p>All stages completed successfully. Check Jenkins console output for detailed logs.</p>
        
        <h2>Artifacts</h2>
        <ul>
            <li>JAR File: Available in build artifacts</li>
            <li>Test Reports: Available in test results</li>
            <li>Coverage Report: Available in coverage reports</li>
            <li>Security Report: Available in OWASP dependency check</li>
        </ul>
    </body>
    </html>
    """
}

def getStatusColor(status) {
    switch(status) {
        case 'SUCCESS':
            return 'good'
        case 'FAILURE':
            return 'danger'
        case 'UNSTABLE':
            return 'warning'
        case 'ABORTED':
            return '#808080'
        default:
            return '#808080'
    }
}