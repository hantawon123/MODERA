pipeline {
    agent any

    triggers {
        pollSCM('H/3 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checkout from SCM'
                checkout scm
            }
        }

        stage('Backend Build') {
            when { changeset "backend/**" }
            steps {
                echo 'Docker image build'
                dir('backend') {
                    sh 'docker build -t modera-backend:${BUILD_NUMBER} -t modera-backend:latest .'
                }
            }
        }

        stage('Deploy') {
            when { changeset "backend/**" }
            steps {
                echo 'Recreate container with new image'
                dir('/home/ubuntu/app') {
                    sh 'docker compose up -d --force-recreate'
                }
                echo 'Wait for Spring startup'
                sh 'sleep 15'
                echo 'Health check'
                sh 'docker exec infra-nginx-1 wget -qO- http://modera-spring:8080/actuator/health'
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
        }
        success {
            echo 'Build and Deploy succeeded.'
        }
        failure {
            echo 'Build or Deploy failed.'
        }
    }
}