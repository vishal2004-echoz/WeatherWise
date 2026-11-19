
pipeline {
    agent any

    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    environment {
        IMAGE_NAME = 'docker.io/vishal202004/weatherwise'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    triggers {
        // Check GitHub for changes every 5 minutes
        pollSCM('H/5 * * * *')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Check Podman') {
            steps {
                sh '''
                    podman --version
                '''
            }
        }

        stage('Build and Test') {
            steps {
                sh '''
                    mvn clean verify
                '''
            }
        }

        stage('Docker Hub Authentication') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_TOKEN'
                    )
                ]) {
                    sh '''
                        set +x
                        printf '%s' "$DOCKER_TOKEN" | \
                        podman login docker.io \
                        --username "$DOCKER_USER" \
                        --password-stdin
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    podman build \
                    -t ${IMAGE_NAME}:latest \
                    -t ${IMAGE_NAME}:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Push Docker Image') {
            steps {
                sh '''
                    podman push ${IMAGE_NAME}:latest
                    podman push ${IMAGE_NAME}:${BUILD_NUMBER}
                '''
            }
        }
    }

    post {
        always {
            sh '''
                podman logout docker.io || true
            '''
        }

        success {
            echo 'WeatherWise CI/CD pipeline completed successfully!'
        }

        failure {
            echo 'WeatherWise CI/CD pipeline failed. Check the console output.'
        }
    }
}
