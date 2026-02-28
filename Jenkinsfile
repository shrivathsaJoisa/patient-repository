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
    COMPOSE_PROJECT_NAME = "patient-mgmt"
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Docker Tools') {
      steps {
        sh 'docker --version'
        sh 'docker-compose version'
      }
    }

    stage('Compose Validate') {
      steps {
        sh 'docker-compose config > /dev/null'
      }
    }

    stage('Compose Build') {
      steps {
        sh 'docker-compose build'
      }
    }

    stage('Deploy') {
      steps {
        sh 'docker-compose up -d'
      }
    }
  }

post {
  failure {
    sh 'docker-compose logs'
  }
}