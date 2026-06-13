pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        IMAGE_NAME = 'fundflow'
        AWS_REGION = 'ap-south-1'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHA = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                }
                echo "Building commit ${env.GIT_SHA}"
            }
        }

        stage('Test') {
            steps {
                // Maven wrapper pins the Maven version; Testcontainers uses the
                // host Docker daemon through the mounted socket.
                sh './mvnw -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build image') {
            steps {
                sh 'docker build -t ${IMAGE_NAME}:${GIT_SHA} .'
            }
        }

        stage('Push to ECR') {
            // Runs only once AWS_ACCOUNT_ID is set as a Jenkins global env var
            // (Manage Jenkins -> System -> Global properties). Until then the
            // stage is skipped and the pipeline stays green.
            when {
                expression { return env.AWS_ACCOUNT_ID?.trim() }
            }
            environment {
                // Secret-text credentials, stored in Jenkins -- never in the repo.
                AWS_ACCESS_KEY_ID     = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                ECR_REGISTRY          = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
            }
            steps {
                sh '''
                    aws ecr get-login-password --region ${AWS_REGION} \
                        | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    docker tag ${IMAGE_NAME}:${GIT_SHA} ${ECR_REGISTRY}/${IMAGE_NAME}:${GIT_SHA}
                    docker tag ${IMAGE_NAME}:${GIT_SHA} ${ECR_REGISTRY}/${IMAGE_NAME}:latest
                    docker push ${ECR_REGISTRY}/${IMAGE_NAME}:${GIT_SHA}
                    docker push ${ECR_REGISTRY}/${IMAGE_NAME}:latest
                '''
            }
        }

        stage('Deploy to EC2') {
            // Needs AWS_ACCOUNT_ID and EC2_HOST as Jenkins global env vars,
            // plus the credentials below. Skipped until all are configured.
            when {
                expression { return env.AWS_ACCOUNT_ID?.trim() && env.EC2_HOST?.trim() }
            }
            environment {
                AWS_ACCESS_KEY_ID     = credentials('aws-access-key-id')
                AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
                SSH_KEY_PATH          = credentials('ec2-ssh-key')      // kind: Secret file (.pem)
                DB_URL                = credentials('rds-db-url')       // kind: Secret text
                DB_USERNAME           = credentials('rds-db-username')  // kind: Secret text
                DB_PASSWORD           = credentials('rds-db-password')  // kind: Secret text
            }
            steps {
                sh 'chmod +x scripts/deploy.sh && ./scripts/deploy.sh ${GIT_SHA}'
            }
        }
    }

    post {
        success {
            echo "Pipeline OK: ${IMAGE_NAME}:${GIT_SHA}"
        }
        cleanup {
            // Don't let CI images pile up on the local daemon.
            sh 'docker image prune -f --filter "label=stage=build" || true'
        }
    }
}
