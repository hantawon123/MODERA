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
                echo 'Docker 이미지 빌드'
                dir('backend') {
                    sh 'docker build -t modera-backend:${BUILD_NUMBER} -t modera-backend:latest .'
                }
            }
        }

        stage('Deploy') {
            when { changeset "backend/**" }
            steps {
                echo '새 이미지로 컨테이너 교체'
                dir('/home/ubuntu/app') {
                    sh 'docker compose up -d --force-recreate'
                }
                echo 'Spring 기동 대기'
                sh 'sleep 15'
                echo '헬스체크'
                sh 'docker exec infra-nginx-1 wget -qO- http://modera-spring:8080/actuator/health'
            }
        }
    }