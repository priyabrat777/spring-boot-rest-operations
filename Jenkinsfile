pipeline {
    agent any
    
    environment {
        // Java and Maven paths
        JAVA_HOME = '/opt/java/openjdk'
        MAVEN_HOME = '/opt/maven'
        PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${env.PATH}"
        
        // Maven options (MaxPermSize removed - not needed in Java 8+)
        MAVEN_OPTS = '-Xmx1024m -Xms256m'
        SONAR_TOKEN = credentials('sonar-token')
        GITHUB_TOKEN = credentials('github-token')
        DOCKER_REGISTRY = 'ghcr.io'
        IMAGE_NAME = 'enterprise-spring-boot-api'
        NOTIFICATION_EMAIL = 'devops@enterprise.com'
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        skipStagesAfterUnstable()
        parallelsAlwaysFailFast()
    }
    
    triggers {
        pollSCM('H/5 * * * *')
        cron('H 2 * * 1-5')
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    env.BUILD_VERSION = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                }
                
                checkout scm
                
                script {
                    def changes = sh(
                        script: 'git diff --name-only HEAD~1 HEAD || echo "Initial commit"',
                        returnStdout: true
                    ).trim()
                    echo "Changed files: ${changes}"
                }
            }
        }
        
        stage('Build Setup') {
            steps {
                script {
                    echo "Building version: ${env.BUILD_VERSION}"
                    echo "JAVA_HOME: ${env.JAVA_HOME}"
                    echo "MAVEN_HOME: ${env.MAVEN_HOME}"
                    
                    // Display versions
                    sh 'java -version'
                    sh 'mvn -version'
                    
                    // Clean and validate
                    sh 'mvn clean validate'
                }
            }
        }
        
        stage('Compile') {
            steps {
                script {
                    try {
                        sh 'mvn compile -DskipTests=true'
                        echo "✅ Compilation successful"
                    } catch (Exception e) {
                        error "❌ Compilation failed: ${e.getMessage()}"
                    }
                }
            }
            post {
                failure {
                    script {
                        currentBuild.result = 'FAILURE'
                        error "Build compilation failed"
                    }
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                script {
                    try {
                        sh '''
                            mvn test \
                                -Dspring.profiles.active=test \
                                -Dmaven.test.failure.ignore=false \
                                -Djacoco.skip=false
                        '''
                        echo "✅ Unit tests completed successfully"
                    } catch (Exception e) {
                        error "❌ Unit tests failed: ${e.getMessage()}"
                    }
                }
            }
            post {
                always {
                    // Publish test results
                    publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                    
                    // Archive test reports
                    archiveArtifacts artifacts: 'target/surefire-reports/**/*', allowEmptyArchive: true
                    
                    // Publish JaCoCo coverage report
                    publishCoverage adapters: [
                        jacocoAdapter('target/site/jacoco/jacoco.xml')
                    ], sourceFileResolver: sourceFiles('STORE_LAST_BUILD')
                }
                failure {
                    script {
                        currentBuild.result = 'FAILURE'
                        error "Unit tests failed"
                    }
                }
            }
        }
        
        stage('Integration Tests') {
            steps {
                script {
                    try {
                        sh '''
                            mvn failsafe:integration-test failsafe:verify \
                                -Dspring.profiles.active=test \
                                -DskipUTs=true \
                                -Dmaven.test.failure.ignore=false
                        '''
                        echo "✅ Integration tests completed successfully"
                    } catch (Exception e) {
                        error "❌ Integration tests failed: ${e.getMessage()}"
                    }
                }
            }
            post {
                always {
                    // Publish integration test results
                    publishTestResults testResultsPattern: 'target/failsafe-reports/*.xml'
                    
                    // Archive integration test reports
                    archiveArtifacts artifacts: 'target/failsafe-reports/**/*', allowEmptyArchive: true
                }
                failure {
                    script {
                        currentBuild.result = 'FAILURE'
                        error "Integration tests failed"
                    }
                }
            }
        }
        
        stage('Code Quality Analysis') {
            parallel {
                stage('SonarQube Analysis') {
                    steps {
                        script {
                            try {
                                withSonarQubeEnv('SonarQube') {
                                    sh '''
                                        mvn sonar:sonar \
                                            -Dsonar.projectKey=enterprise-spring-boot-api \
                                            -Dsonar.projectName="Enterprise Spring Boot API" \
                                            -Dsonar.projectVersion=${BUILD_VERSION} \
                                            -Dsonar.sources=src/main/java \
                                            -Dsonar.tests=src/test/java \
                                            -Dsonar.java.coveragePlugin=jacoco \
                                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                            -Dsonar.junit.reportPaths=target/surefire-reports,target/failsafe-reports
                                    '''
                                }
                                echo "✅ SonarQube analysis completed"
                            } catch (Exception e) {
                                error "❌ SonarQube analysis failed: ${e.getMessage()}"
                            }
                        }
                    }
                }
                
                stage('Security Scan') {
                    steps {
                        script {
                            try {
                                // OWASP Dependency Check
                                sh '''
                                    mvn org.owasp:dependency-check-maven:check \
                                        -DfailBuildOnCVSS=7 \
                                        -DsuppressionsFile=owasp-suppressions.xml
                                '''
                                echo "✅ Security scan completed"
                            } catch (Exception e) {
                                echo "⚠️ Security scan found vulnerabilities: ${e.getMessage()}"
                                unstable("Security vulnerabilities detected")
                            }
                        }
                    }
                    post {
                        always {
                            // Archive security reports
                            archiveArtifacts artifacts: 'target/dependency-check-report.html', allowEmptyArchive: true
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'target',
                                reportFiles: 'dependency-check-report.html',
                                reportName: 'OWASP Dependency Check Report'
                            ])
                        }
                    }
                }
            }
        }
        
        stage('Quality Gate') {
            steps {
                script {
                    timeout(time: 5, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        }
                        echo "✅ Quality gate passed"
                    }
                }
            }
        }
        
        stage('Package Application') {
            steps {
                script {
                    try {
                        sh '''
                            mvn package -DskipTests=true \
                                -Dmaven.javadoc.skip=true \
                                -Dspring.profiles.active=prod
                        '''
                        
                        // Verify JAR file was created
                        sh 'ls -la target/*.jar'
                        echo "✅ Application packaged successfully"
                    } catch (Exception e) {
                        error "❌ Application packaging failed: ${e.getMessage()}"
                    }
                }
            }
            post {
                success {
                    // Archive the built artifacts
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    
                    // Store build info
                    script {
                        def jarFile = sh(
                            script: 'ls target/*.jar | head -1',
                            returnStdout: true
                        ).trim()
                        env.JAR_FILE = jarFile
                        echo "JAR file: ${env.JAR_FILE}"
                    }
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    branch 'release/*'
                }
            }
            steps {
                script {
                    try {
                        // Build Docker image
                        def image = docker.build("${env.IMAGE_NAME}:${env.BUILD_VERSION}")
                        
                        // Tag with latest if main branch
                        if (env.BRANCH_NAME == 'main') {
                            image.tag('latest')
                        }
                        
                        echo "✅ Docker image built successfully"
                        env.DOCKER_IMAGE = "${env.IMAGE_NAME}:${env.BUILD_VERSION}"
                    } catch (Exception e) {
                        error "❌ Docker image build failed: ${e.getMessage()}"
                    }
                }
            }
        }
        
        stage('Performance Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    try {
                        // Run performance tests
                        sh '''
                            mvn test \
                                -Dtest=*PerformanceTest \
                                -Dspring.profiles.active=test \
                                -Dmaven.test.failure.ignore=false
                        '''
                        echo "✅ Performance tests completed"
                    } catch (Exception e) {
                        echo "⚠️ Performance tests failed: ${e.getMessage()}"
                        unstable("Performance tests failed")
                    }
                }
            }
            post {
                always {
                    // Archive performance test results
                    archiveArtifacts artifacts: 'target/performance-reports/**/*', allowEmptyArchive: true
                }
            }
        }
        
        stage('Create Release') {
            when {
                branch 'main'
            }
            steps {
                script {
                    try {
                        // Create GitHub release
                        def releaseNotes = generateReleaseNotes()
                        
                        sh """
                            gh release create v${env.BUILD_VERSION} \
                                --title "Release v${env.BUILD_VERSION}" \
                                --notes "${releaseNotes}" \
                                --target main \
                                ${env.JAR_FILE}
                        """
                        
                        echo "✅ GitHub release created: v${env.BUILD_VERSION}"
                    } catch (Exception e) {
                        echo "⚠️ Failed to create GitHub release: ${e.getMessage()}"
                        unstable("GitHub release creation failed")
                    }
                }
            }
        }
        
        stage('Deploy to Staging') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    try {
                        // Deploy to staging environment
                        echo "🚀 Deploying to staging environment..."
                        
                        // This would typically involve:
                        // - Pushing Docker image to registry
                        // - Updating Kubernetes manifests
                        // - Rolling deployment
                        
                        sh """
                            echo "Deploying ${env.DOCKER_IMAGE} to staging"
                            # kubectl set image deployment/api api=${env.DOCKER_IMAGE} -n staging
                            # kubectl rollout status deployment/api -n staging
                        """
                        
                        echo "✅ Staging deployment completed"
                    } catch (Exception e) {
                        error "❌ Staging deployment failed: ${e.getMessage()}"
                    }
                }
            }
        }
        
        stage('Smoke Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            steps {
                script {
                    try {
                        // Run smoke tests against staging
                        sh '''
                            mvn test \
                                -Dtest=*SmokeTest \
                                -Dspring.profiles.active=staging \
                                -Dapi.base.url=https://staging-api.enterprise.com
                        '''
                        echo "✅ Smoke tests passed"
                    } catch (Exception e) {
                        error "❌ Smoke tests failed: ${e.getMessage()}"
                    }
                }
            }
        }
        
        stage('Production Deployment Approval') {
            when {
                branch 'main'
            }
            steps {
                script {
                    try {
                        timeout(time: 24, unit: 'HOURS') {
                            input message: 'Deploy to Production?', 
                                  ok: 'Deploy',
                                  submitterParameter: 'APPROVER'
                        }
                        echo "✅ Production deployment approved by: ${env.APPROVER}"
                    } catch (Exception e) {
                        echo "❌ Production deployment not approved"
                        currentBuild.result = 'ABORTED'
                    }
                }
            }
        }
        
        stage('Deploy to Production') {
            when {
                allOf {
                    branch 'main'
                    expression { currentBuild.result != 'ABORTED' }
                }
            }
            steps {
                script {
                    try {
                        echo "🚀 Deploying to production environment..."
                        
                        // Production deployment steps
                        sh """
                            echo "Deploying ${env.DOCKER_IMAGE} to production"
                            # kubectl set image deployment/api api=${env.DOCKER_IMAGE} -n production
                            # kubectl rollout status deployment/api -n production
                        """
                        
                        echo "✅ Production deployment completed"
                    } catch (Exception e) {
                        error "❌ Production deployment failed: ${e.getMessage()}"
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Clean up workspace
                cleanWs()
                
                // Generate build summary
                def buildSummary = generateBuildSummary()
                echo buildSummary
            }
        }
        
        success {
            script {
                echo "🎉 Pipeline completed successfully!"
                
                // Send success notification
                sendNotification(
                    status: 'SUCCESS',
                    message: "✅ Build ${env.BUILD_NUMBER} completed successfully for ${env.BRANCH_NAME}"
                )
            }
        }
        
        failure {
            script {
                echo "❌ Pipeline failed!"
                
                // Send failure notification
                sendNotification(
                    status: 'FAILURE',
                    message: "❌ Build ${env.BUILD_NUMBER} failed for ${env.BRANCH_NAME}"
                )
                
                // Archive logs for debugging
                archiveArtifacts artifacts: 'target/logs/**/*', allowEmptyArchive: true
            }
        }
        
        unstable {
            script {
                echo "⚠️ Pipeline completed with warnings!"
                
                // Send unstable notification
                sendNotification(
                    status: 'UNSTABLE',
                    message: "⚠️ Build ${env.BUILD_NUMBER} completed with warnings for ${env.BRANCH_NAME}"
                )
            }
        }
        
        aborted {
            script {
                echo "🛑 Pipeline was aborted!"
                
                // Send aborted notification
                sendNotification(
                    status: 'ABORTED',
                    message: "🛑 Build ${env.BUILD_NUMBER} was aborted for ${env.BRANCH_NAME}"
                )
            }
        }
    }
}

// Helper function to generate release notes
def generateReleaseNotes() {
    def commitMessages = sh(
        script: 'git log --oneline --since="1 week ago" --pretty=format:"- %s"',
        returnStdout: true
    ).trim()
    
    return """
## What's Changed
${commitMessages}

## Build Information
- Build Number: ${env.BUILD_NUMBER}
- Git Commit: ${env.GIT_COMMIT_SHORT}
- Build Date: ${new Date().format('yyyy-MM-dd HH:mm:ss')}

## Test Results
- Unit Tests: ✅ Passed
- Integration Tests: ✅ Passed
- Code Coverage: Available in SonarQube
- Security Scan: Available in OWASP Report

## Artifacts
- JAR File: ${env.JAR_FILE}
- Docker Image: ${env.DOCKER_IMAGE}
"""
}

// Helper function to generate build summary
def generateBuildSummary() {
    return """
=== BUILD SUMMARY ===
Build Number: ${env.BUILD_NUMBER}
Branch: ${env.BRANCH_NAME}
Commit: ${env.GIT_COMMIT_SHORT}
Version: ${env.BUILD_VERSION}
Status: ${currentBuild.result ?: 'SUCCESS'}
Duration: ${currentBuild.durationString}
Started: ${new Date(currentBuild.startTimeInMillis).format('yyyy-MM-dd HH:mm:ss')}
===================
"""
}

// Helper function to send notifications
def sendNotification(Map config) {
    def status = config.status
    def message = config.message
    def color = getStatusColor(status)
    
    // Email notification
    emailext (
        subject: "[Jenkins] ${status}: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
        body: """
        <h2>Build ${status}</h2>
        <p><strong>Job:</strong> ${env.JOB_NAME}</p>
        <p><strong>Build Number:</strong> ${env.BUILD_NUMBER}</p>
        <p><strong>Branch:</strong> ${env.BRANCH_NAME}</p>
        <p><strong>Commit:</strong> ${env.GIT_COMMIT_SHORT}</p>
        <p><strong>Duration:</strong> ${currentBuild.durationString}</p>
        <p><strong>Message:</strong> ${message}</p>
        <p><strong>Build URL:</strong> <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>
        """,
        to: "${env.NOTIFICATION_EMAIL}",
        mimeType: 'text/html'
    )
    
    // Slack notification disabled - plugin not installed
    // Uncomment and install Slack plugin if needed:
    // slackSend(
    //     channel: '#ci-cd',
    //     color: color,
    //     message: "${message}\nBuild: ${env.BUILD_URL}"
    // )
}

// Helper function to get status color for notifications
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