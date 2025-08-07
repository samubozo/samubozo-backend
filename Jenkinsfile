// ======================================================
// 최종 Jenkinsfile (NullPointerException 해결)
// ======================================================

def deployHost = "172.31.9.208"

pipeline {
    agent any
    environment {
        SERVICE_DIRS = "approval-service,attendance-service,auth-service,certificate-service,chatbot-service,config-service,discovery-service,gateway-service,hr-service,message-service,notification-service,payroll-service,schedule-service,vacation-service"
        ECR_URL = "886331869898.dkr.ecr.ap-northeast-2.amazonaws.com"
        REGION = "ap-northeast-2"
        CHANGED_SERVICES = ""
        COMMON_MODULES = "common-module,parent-module"
    }
    stages {
        stage('Pull Codes from Github') {
            steps {
                checkout scm
            }
        }
        stage('Add Secret To config-service') {
            steps {
                withCredentials([file(credentialsId: 'config-secret', variable: 'configSecret')]) {
                    script {
                        sh 'cp $configSecret config-service/src/main/resources/application-dev.yml'
                    }
                }
            }
        }
        stage('Detect Changes') {
            steps {
                script {
                    def allServices = env.SERVICE_DIRS.split(",").toList()
                    def commonModules = env.COMMON_MODULES.split(",").toList()
                    def changedServices = []
                    def shouldBuildAll = false

                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        try {
                            def repoListJson = sh(script: "aws ecr describe-repositories --output json", returnStdout: true)
                            def repoList = new groovy.json.JsonSlurper().parseText(repoListJson)
                            if (repoList.repositories.isEmpty()) {
                                echo "No ECR repositories found. Building all services for the first time."
                                shouldBuildAll = true
                            }
                        } catch (Exception e) {
                            echo "Failed to check ECR repositories: ${e.getMessage()}. Assuming it's the first build."
                            shouldBuildAll = true
                        }
                    }

                    if (shouldBuildAll) {
                        changedServices = allServices
                    } else {
                        def commitCount = sh(script: "git rev-list --count HEAD", returnStdout: true).trim().toInteger()
                        if (commitCount == 1) {
                            echo "Initial commit detected. All services will be built."
                            changedServices = allServices
                        } else {
                            def changedFilesOutput = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()

                            if (changedFilesOutput == "") {
                                echo "No changes detected."
                            } else {
                                def changedFiles = changedFilesOutput.split('\n').toList()
                                echo "Changed files: ${changedFiles}"

                                if (commonModules.any { module -> changedFiles.any { it.startsWith(module + "/") } }) {
                                    echo "Common module changed. Building all services."
                                    changedServices = allServices
                                } else {
                                    allServices.each { service ->
                                        if (changedFiles.any { it.startsWith(service + "/") }) {
                                            changedServices.add(service)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "No changes in service directories. Skipping build and deployment."
                        env.CHANGED_SERVICES = ""
                        currentBuild.result = 'SUCCESS'
                    } else {
                        env.CHANGED_SERVICES = changedServices.unique().join(",")
                    }
                    echo "Services to be built: ${env.CHANGED_SERVICES}"
                }
            }
        }

        stage('Build & Push Changed Services in Parallel') {
            when {
                // ✨ 수정 1: when 조건을 강화. Groovy에서는 null과 빈 문자열 모두 false로 취급되므로, 이렇게만 써도 충분합니다.
                expression { env.CHANGED_SERVICES }
            }
            steps {
                withAWS(region: "${REGION}", credentials: "aws-key") {
                    script {
                        // ✨ 수정 2: Elvis 연산자(?: '')를 사용해 만약 env.CHANGED_SERVICES가 null이더라도 빈 문자열로 처리하여 NullPointerException 방지
                        def changedServices = (env.CHANGED_SERVICES ?: '').split(",").toList()
                        def parallelTasks = [:]

                        sh "aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL}"

                        changedServices.each { service ->
                            parallelTasks["Build & Push ${service}"] = {
                                sh """
                                    echo "--- Building ${service} ---"
                                    cd ${service}
                                    ./gradlew clean build -x test
                                    cd ..

                                    echo "--- Building and Pushing Docker image for ${service} ---"
                                    docker build -t ${service}:latest ./${service}
                                    docker tag ${service}:latest ${ECR_URL}/${service}:latest
                                    docker push ${ECR_URL}/${service}:latest
                                """
                            }
                        }
                        parallel parallelTasks
                    }
                }
            }
        }
//         stage('Deploy Changed Services to AWS EC2') {
//             when {
//                 // ✨ 수정 1: when 조건을 강화
//                 expression { env.CHANGED_SERVICES }
//             }
//             steps {
//                 sshagent(credentials: ["deploy-key"]) {
//                     sh """
//                         echo "[INFO] SCP docker-compose.yml 전송 중..."
//                         scp -o StrictHostKeyChecking=no docker-compose.yml ubuntu@${deployHost}:/home/ubuntu/docker-compose.yml
//
//                         echo "[INFO] SSH 접속 및 배포 실행..."
//                         ssh -o StrictHostKeyChecking=no ubuntu@${deployHost} '
//                             set -e
//
//                             echo "[INFO] ECR 로그인 중..."
//                             aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL}
//
//                             cd /home/ubuntu
//
//                             CHANGED_SERVICES_FOR_COMPOSE="${env.CHANGED_SERVICES.replace(",", " ")}"
//
//                             echo "[INFO] 이미지 Pull 중: ${CHANGED_SERVICES_FOR_COMPOSE}"
//                             docker-compose pull ${CHANGED_SERVICES_FOR_COMPOSE}
//
//                             echo "[INFO] 서비스 재시작 중..."
//                             docker-compose up -d --remove-orphans ${CHANGED_SERVICES_FOR_COMPOSE}
//                         '
//                     """
//                 }
//             }
//         }
    }
}