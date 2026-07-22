pipeline {
    agent any

    triggers {
        pollSCM('H/3 * * * *')   // 3분마다 GitLab 확인
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checkout from SCM (잡 설정의 Git 사용)'
                checkout scm
            }
        }

        stage('Backend Build') {
            when { changeset "backend/**" }
            steps {
                echo 'Docker 이미지 빌드 (내부에서 gradle bootJar)'
                dir('backend') {
                    sh 'docker build -t modera-backend:${BUILD_NUMBER} -t modera-backend:latest .'
                }
            }
        }
    }

    post {
        always { echo 'Pipeline finished.' }
        success { echo 'Build succeeded.' }
        failure { echo 'Build failed.' }
    }
}