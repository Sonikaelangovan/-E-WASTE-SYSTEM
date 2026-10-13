pipeline {
  agent any
  options { timestamps(); skipDefaultCheckout(true) }
  stages {
    stage('Checkout') { steps { checkout scm } }
    stage('Frontend build') { steps { dir('frontend') { sh 'npm ci'; sh 'npm run build' } } }
    stage('Backend tests') { steps { dir('backend') { sh 'mvn -B test' } } }
    stage('Docker build') { when { branch 'main' } steps { sh 'docker compose build' } }
  }
  post { always { junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'; archiveArtifacts artifacts: 'frontend/dist/**,backend/target/*.jar', allowEmptyArchive: true } }
}
