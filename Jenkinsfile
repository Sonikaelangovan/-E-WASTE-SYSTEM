pipeline {
  agent { label 'linux-docker' }

  options {
    timestamps()
    skipDefaultCheckout(true)
    disableConcurrentBuilds()
  }

  parameters {
    booleanParam(name: 'PUBLISH_IMAGES', defaultValue: false, description: 'Push commit-tagged images to the configured container registry (main branch only).')
    booleanParam(name: 'DEPLOY_ENABLED', defaultValue: false, description: 'After publishing, request approval and deploy the images to the configured Ansible hosts.')
    string(name: 'REGISTRY_HOST', defaultValue: 'docker.io', description: 'Container registry host, for example docker.io or registry.example.com.')
    string(name: 'IMAGE_NAMESPACE', defaultValue: '', description: 'Registry namespace or organization. Required to publish images.')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.BUILD_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
          echo "Building commit ${env.BUILD_COMMIT}"
        }
      }
    }

    stage('Frontend checks') {
      steps {
        dir('frontend') {
          sh 'npm ci'
          sh 'npm run lint'
          sh 'npm run build'
        }
      }
    }

    stage('Backend verification') {
      steps {
        dir('backend') { sh 'mvn -B verify' }
      }
    }

    stage('Build container images') {
      steps {
        sh 'docker build -t loopcycle-backend:${BUILD_COMMIT} backend'
        sh 'docker build --build-arg VITE_API_BASE_URL=/api -t loopcycle-frontend:${BUILD_COMMIT} frontend'
      }
    }

    stage('Publish images') {
      when {
        allOf { branch 'main'; expression { params.PUBLISH_IMAGES } }
      }
      steps {
        script {
          def registry = params.REGISTRY_HOST.trim()
          def namespace = params.IMAGE_NAMESPACE.trim().replaceAll('^/+|/+$', '')
          if (!(registry ==~ /[A-Za-z0-9.-]+(?::[0-9]+)?/) || !namespace || !(namespace ==~ /[a-z0-9]+([._-][a-z0-9]+)*(\/[a-z0-9]+([._-][a-z0-9]+)*)*/)) {
            error('Set a valid REGISTRY_HOST and lowercase IMAGE_NAMESPACE before publishing.')
          }
          env.BACKEND_REMOTE_IMAGE = "${registry}/${namespace}/loopcycle-backend"
          env.FRONTEND_REMOTE_IMAGE = "${registry}/${namespace}/loopcycle-frontend"
          env.REGISTRY_HOST_NORMALIZED = registry
          env.IMAGE_NAMESPACE_NORMALIZED = namespace
          sh 'docker tag loopcycle-backend:${BUILD_COMMIT} ${BACKEND_REMOTE_IMAGE}:${BUILD_COMMIT}'
          sh 'docker tag loopcycle-frontend:${BUILD_COMMIT} ${FRONTEND_REMOTE_IMAGE}:${BUILD_COMMIT}'
          withCredentials([usernamePassword(credentialsId: 'loopcycle-registry', usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD')]) {
            sh '''
              set +x
              trap 'docker logout "$REGISTRY_HOST" >/dev/null 2>&1 || true' EXIT
              printf '%s' "$REGISTRY_PASSWORD" | docker login "$REGISTRY_HOST" --username "$REGISTRY_USERNAME" --password-stdin
              docker push "$BACKEND_REMOTE_IMAGE:$BUILD_COMMIT"
              docker push "$FRONTEND_REMOTE_IMAGE:$BUILD_COMMIT"
            '''
          }
        }
      }
    }

    stage('Approve deployment') {
      when {
        allOf { branch 'main'; expression { params.DEPLOY_ENABLED } }
      }
      steps {
        script {
          if (!params.PUBLISH_IMAGES) error('DEPLOY_ENABLED requires PUBLISH_IMAGES.')
          timeout(time: 15, unit: 'MINUTES') {
            input message: "Deploy ${env.BUILD_COMMIT} to the configured LoopCycle hosts?", ok: 'Deploy'
          }
        }
      }
    }

    stage('Deploy with Ansible') {
      when {
        allOf { branch 'main'; expression { params.DEPLOY_ENABLED } }
      }
      steps {
        script {
          def registry = params.REGISTRY_HOST.trim()
          def namespace = params.IMAGE_NAMESPACE.trim().replaceAll('^/+|/+$', '')
          withCredentials([
            file(credentialsId: 'loopcycle-deploy-env', variable: 'DEPLOY_ENV_FILE'),
            file(credentialsId: 'loopcycle-ansible-inventory', variable: 'ANSIBLE_INVENTORY'),
            usernamePassword(credentialsId: 'loopcycle-registry', usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD')
          ]) {
            sshagent(credentials: ['loopcycle-deploy-ssh']) {
              sh '''
                set -eu
                install -m 600 "$DEPLOY_ENV_FILE" .env
                trap 'rm -f .env' EXIT
                ansible-playbook -i "$ANSIBLE_INVENTORY" ansible/playbooks/docker.yml --syntax-check
                ansible-playbook -i "$ANSIBLE_INVENTORY" ansible/playbooks/webserver.yml --syntax-check
                ansible-playbook -i "$ANSIBLE_INVENTORY" ansible/playbooks/docker.yml \
                  -e "image_registry=$REGISTRY_HOST_NORMALIZED image_namespace=$IMAGE_NAMESPACE_NORMALIZED image_tag=$BUILD_COMMIT"
                ansible-playbook -i "$ANSIBLE_INVENTORY" ansible/playbooks/webserver.yml
              '''
            }
          }
        }
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
      archiveArtifacts artifacts: 'frontend/dist/**,backend/target/*.jar', allowEmptyArchive: true
    }
  }
}
