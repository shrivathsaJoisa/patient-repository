pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  environment {
    DOCKER_BUILDKIT = '1'
    COMPOSE_DOCKER_CLI_BUILD = '1'
    COMPOSE_PROJECT_NAME = "patient-mgmt-${BUILD_NUMBER}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Docker Tools') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker --version'
            sh 'docker compose version'
          } else {
            bat 'docker --version'
            bat 'docker compose version'
          }
        }
      }
    }

    stage('Compose Validate') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker compose config > /dev/null'
          } else {
            bat 'docker compose config > nul'
          }
        }
      }
    }

    stage('Compose Build') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker compose build'
          } else {
            bat 'docker compose build'
          }
        }
      }
    }
  }

  post {
    always {
      script {
        if (isUnix()) {
          sh 'docker compose down -v --remove-orphans || true'
        } else {
          bat 'docker compose down -v --remove-orphans'
        }
      }
    }
  }
}
