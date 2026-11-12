pipeline {
    agent any

    tools {
        maven 'Maven3'   // Manage Jenkins > Tools: name these exactly
        jdk   'JDK17'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        pollSCM('H/5 * * * *')   // or use a GitHub webhook
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Build') {
            steps { sh 'mvn -B clean compile' }
        }
        stage('Test') {
            steps { sh 'mvn -B test' }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }
        stage('Package') {
            steps { sh 'mvn -B package -DskipTests' }
        }
        stage('Archive') {
            steps { archiveArtifacts artifacts: 'target/*.jar', fingerprint: true }
        }
    }

    post {
        success {
            mail to: 'your-email@example.com',
                 subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build passed.\n${env.BUILD_URL}"
        }
        failure {
            mail to: 'your-email@example.com',
                 subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed. Check the console output:\n${env.BUILD_URL}console"
        }
    }
}
