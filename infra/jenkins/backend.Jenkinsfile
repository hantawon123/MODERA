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

        stage('Gradle Build') {
            when { changeset "backend/**" }
            steps {
                dir('backend') {
                    sh '''
                        chmod +x gradlew
                        ./gradlew clean :api-server:bootJar :analysis-worker:bootJar 2>&1
                    '''
                }
            }
        }

        stage('Docker Build') {
            when { changeset "backend/**" }
            steps {
                sh 'docker build -t modera-api:${BUILD_NUMBER} -t modera-api:latest backend/api-server 2>&1'
                sh 'docker build -t modera-worker:${BUILD_NUMBER} -t modera-worker:latest backend/analysis-worker 2>&1'
            }
        }

        stage('Deploy') {
            when { changeset "backend/**" }
            steps {
                echo 'Deploy: api (blue-green, zero-downtime)'
                sh 'bash infra/deploy-api.sh ${BUILD_NUMBER}'
                echo 'Deploy: worker'
                sh 'cd /home/ubuntu/app/spring && docker compose up -d --force-recreate modera-worker 2>&1'
                sh 'docker ps --filter name=modera- --format "{{.Names}} {{.Status}}"'
            }
        }

    }

    post {
        always { echo 'Pipeline finished.' }
        success { echo 'Build and Deploy succeeded.' }
        failure { echo 'Build or Deploy failed.' }
    }
}