/**
 * Jenkins Job DSL for Enterprise Spring Boot API Pipeline
 * This script creates the Jenkins pipeline job configuration
 */

// Main pipeline job
pipelineJob('enterprise-spring-boot-api') {
    displayName('Enterprise Spring Boot API - CI/CD Pipeline')
    description('Complete CI/CD pipeline for Enterprise Spring Boot API with comprehensive testing, quality analysis, and deployment')
    
    // Job properties
    properties {
        buildDiscarder {
            strategy {
                logRotator {
                    numToKeepStr('10')
                    artifactNumToKeepStr('5')
                    daysToKeepStr('30')
                }
            }
        }
        
        githubProjectProperty {
            projectUrl('https://github.com/enterprise/spring-boot-api')
        }
        
        pipelineTriggers {
            triggers {
                githubPush()
                pollSCM {
                    scmpoll_spec('H/5 * * * *')
                }
                cron {
                    spec('H 2 * * 1-5')
                }
            }
        }
    }
    
    // Parameters
    parameters {
        booleanParam('SKIP_TESTS', false, 'Skip running tests during build')
        choiceParam('DEPLOY_ENVIRONMENT', ['staging', 'production'], 'Target deployment environment')
        booleanParam('FORCE_DEPLOY', false, 'Force deployment even if quality gates fail')
        stringParam('MAVEN_GOALS', 'clean compile test package', 'Maven goals to execute')
        stringParam('DOCKER_TAG', '', 'Custom Docker tag (leave empty for auto-generated)')
    }
    
    // Pipeline definition
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/enterprise/spring-boot-api.git')
                        credentials('github-credentials')
                    }
                    branches('*/main', '*/develop', '*/release/*')
                    scriptPath('Jenkinsfile')
                }
            }
            lightweight(true)
        }
    }
    
    // Configure build environment
    configure { project ->
        project / 'properties' / 'jenkins.model.BuildDiscarderProperty' / 'strategy' / 'daysToKeep' << '30'
        project / 'properties' / 'jenkins.model.BuildDiscarderProperty' / 'strategy' / 'numToKeep' << '10'
    }
}

// Multi-branch pipeline for feature branches
multibranchPipelineJob('enterprise-spring-boot-api-multibranch') {
    displayName('Enterprise Spring Boot API - Multi-branch Pipeline')
    description('Multi-branch pipeline for feature branch builds and pull request validation')
    
    branchSources {
        github {
            id('enterprise-spring-boot-api-github')
            scanCredentialsId('github-credentials')
            repoOwner('enterprise')
            repository('spring-boot-api')
            
            buildOriginBranch(true)
            buildOriginBranchWithPR(true)
            buildOriginPRMerge(false)
            buildOriginPRHead(true)
            buildForkPRMerge(false)
            buildForkPRHead(false)
        }
    }
    
    // Branch discovery
    configure { node ->
        def traits = node / 'sources' / 'data' / 'jenkins.branch.BranchSource' / 'source' / 'traits'
        
        // Discover branches
        traits << 'org.jenkinsci.plugins.github__branch__source.BranchDiscoveryTrait' {
            strategyId(1) // Exclude branches that are also filed as PRs
        }
        
        // Discover pull requests
        traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
            strategyId(2) // Merging the pull request with the current target branch revision
        }
        
        // Clean before checkout
        traits << 'jenkins.plugins.git.traits.CleanBeforeCheckoutTrait'
        
        // Checkout over SSH
        traits << 'jenkins.plugins.git.traits.CheckoutOverSSHTrait' {
            credentialsId('github-ssh-key')
        }
    }
    
    // Orphaned item strategy
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(20)
            daysToKeep(30)
        }
    }
    
    // Periodic folder computation
    triggers {
        periodicFolderTrigger {
            interval('1d')
        }
    }
}

// Release pipeline job
pipelineJob('enterprise-spring-boot-api-release') {
    displayName('Enterprise Spring Boot API - Release Pipeline')
    description('Release pipeline for creating and deploying production releases')
    
    parameters {
        stringParam('RELEASE_VERSION', '', 'Release version (e.g., 1.0.0)')
        stringParam('NEXT_VERSION', '', 'Next development version (e.g., 1.1.0-SNAPSHOT)')
        booleanParam('SKIP_TESTS', false, 'Skip tests during release')
        booleanParam('DRY_RUN', true, 'Perform a dry run without actually releasing')
    }
    
    definition {
        cps {
            script('''
                pipeline {
                    agent any
                    
                    tools {
                        maven 'Maven-3.9.0'
                        jdk 'JDK-21'
                    }
                    
                    stages {
                        stage('Validate Release Parameters') {
                            steps {
                                script {
                                    if (!params.RELEASE_VERSION) {
                                        error "RELEASE_VERSION parameter is required"
                                    }
                                    if (!params.NEXT_VERSION) {
                                        error "NEXT_VERSION parameter is required"
                                    }
                                    echo "Preparing release ${params.RELEASE_VERSION}"
                                    echo "Next development version: ${params.NEXT_VERSION}"
                                }
                            }
                        }
                        
                        stage('Checkout') {
                            steps {
                                checkout scm
                            }
                        }
                        
                        stage('Run Tests') {
                            when {
                                not { params.SKIP_TESTS }
                            }
                            steps {
                                sh 'mvn clean test'
                            }
                        }
                        
                        stage('Maven Release') {
                            steps {
                                script {
                                    def releaseGoals = params.DRY_RUN ? 
                                        'release:prepare -DdryRun=true' : 
                                        'release:prepare release:perform'
                                    
                                    sh """
                                        mvn ${releaseGoals} \\
                                            -DreleaseVersion=${params.RELEASE_VERSION} \\
                                            -DdevelopmentVersion=${params.NEXT_VERSION} \\
                                            -Dtag=v${params.RELEASE_VERSION} \\
                                            -DautoVersionSubmodules=true \\
                                            -DskipTests=${params.SKIP_TESTS}
                                    """
                                }
                            }
                        }
                        
                        stage('Create GitHub Release') {
                            when {
                                not { params.DRY_RUN }
                            }
                            steps {
                                script {
                                    sh """
                                        gh release create v${params.RELEASE_VERSION} \\
                                            --title "Release v${params.RELEASE_VERSION}" \\
                                            --notes "Release v${params.RELEASE_VERSION}" \\
                                            --target main \\
                                            target/*.jar
                                    """
                                }
                            }
                        }
                    }
                    
                    post {
                        success {
                            echo "✅ Release ${params.RELEASE_VERSION} completed successfully"
                        }
                        failure {
                            echo "❌ Release ${params.RELEASE_VERSION} failed"
                        }
                    }
                }
            '''.stripIndent())
            sandbox(true)
        }
    }
}

// Nightly build job
pipelineJob('enterprise-spring-boot-api-nightly') {
    displayName('Enterprise Spring Boot API - Nightly Build')
    description('Nightly build with extended testing and performance benchmarks')
    
    triggers {
        cron('H 1 * * *') // Run at 1 AM daily
    }
    
    definition {
        cps {
            script('''
                pipeline {
                    agent any
                    
                    tools {
                        maven 'Maven-3.9.0'
                        jdk 'JDK-21'
                    }
                    
                    stages {
                        stage('Checkout') {
                            steps {
                                checkout scm
                            }
                        }
                        
                        stage('Full Test Suite') {
                            parallel {
                                stage('Unit Tests') {
                                    steps {
                                        sh 'mvn test'
                                    }
                                }
                                stage('Integration Tests') {
                                    steps {
                                        sh 'mvn failsafe:integration-test failsafe:verify'
                                    }
                                }
                                stage('Performance Tests') {
                                    steps {
                                        sh 'mvn test -Dtest=*PerformanceTest'
                                    }
                                }
                            }
                        }
                        
                        stage('Security Scan') {
                            steps {
                                sh 'mvn org.owasp:dependency-check-maven:check'
                            }
                        }
                        
                        stage('Code Quality') {
                            steps {
                                withSonarQubeEnv('SonarQube') {
                                    sh 'mvn sonar:sonar'
                                }
                            }
                        }
                        
                        stage('Generate Reports') {
                            steps {
                                sh 'mvn site'
                            }
                        }
                    }
                    
                    post {
                        always {
                            publishTestResults testResultsPattern: 'target/surefire-reports/*.xml,target/failsafe-reports/*.xml'
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: 'target/site',
                                reportFiles: 'index.html',
                                reportName: 'Maven Site Report'
                            ])
                        }
                        failure {
                            emailext (
                                subject: "Nightly Build Failed: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                                body: "The nightly build has failed. Please check the build logs.",
                                to: "devops@enterprise.com"
                            )
                        }
                    }
                }
            '''.stripIndent())
            sandbox(true)
        }
    }
}

// Folder for organizing jobs
folder('enterprise-spring-boot-api-folder') {
    displayName('Enterprise Spring Boot API')
    description('Folder containing all jobs related to the Enterprise Spring Boot API project')
}

// Move jobs to folder
queue('enterprise-spring-boot-api-folder/enterprise-spring-boot-api')
queue('enterprise-spring-boot-api-folder/enterprise-spring-boot-api-multibranch')
queue('enterprise-spring-boot-api-folder/enterprise-spring-boot-api-release')
queue('enterprise-spring-boot-api-folder/enterprise-spring-boot-api-nightly')