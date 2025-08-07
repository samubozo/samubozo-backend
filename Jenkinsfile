// ======================================================
// 최종 디버깅용 Jenkinsfile
// (모든 실행 명령어와 AWS 자격증명을 로그에 출력)
// ======================================================

def deployHost = "172.31.9.208"

pipeline {
    agent any

    parameters {
        string(name: 'MANUAL_BUILD_SERVICES', defaultValue: '', description: '수동으로 빌드/배포할 서비스 목록 (쉼표로 구분). 비워두면 자동 감지 로직을 따릅니다.')
    }

    environment {
        SERVICE_DIRS = "approval-service,attendance-service,auth-service,certificate-service,chatbot-service,config-service,discovery-service,gateway-service,hr-service,message-service,notification-service,payroll-service,schedule-service,vacation-service"
        ECR_URL = "886331869898.dkr.ecr.ap-northeast-2.amazonaws.com"
        REGION = "ap-northeast-2"
        CHANGED_SERVICES = ""
        COMMON_MODULES = "common-module,parent-module"
    }
    stages {
        stage('Initial Setup') {
            steps {
                checkout scm
                withCredentials([file(credentialsId: 'config-secret', variable: 'configSecret')]) {
                    sh 'cp $configSecret config-service/src/main/resources/application-dev.yml'
                }
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    def allServices = env.SERVICE_DIRS.split(",").toList()
                    def commonModules = env.COMMON_MODULES.split(",").toList()
                    def changedServices = []

                    if (params.MANUAL_BUILD_SERVICES.trim()) {
                        echo "Manual build triggered for: ${params.MANUAL_BUILD_SERVICES}"
                        changedServices = params.MANUAL_BUILD_SERVICES.split(',').collect { it.trim() }

                    } else {
                        echo "--- Checking for services with no images in ECR ---"
                        withAWS(region: "${REGION}", credentials: "aws-key") {
                            // ✨ 디버깅: 현재 어떤 AWS 자격증명으로 실행되는지 확인
                            sh 'aws sts get-caller-identity'

                            allServices.each { service ->
                                def statusCode = sh(script: "aws ecr describe-images --repository-name ${service} --max-items 1 > /dev/null 2>&1", returnStatus: true)
                                if (statusCode != 0) {
                                    echo "-> No images found for '${service}' in ECR (exit code: ${statusCode}). Adding to build list."
                                    changedServices.add(service)
                                }
                            }
                        }

                        echo "\n--- Checking for services with code changes via Git ---"
                        def commitCount = sh(script: "git rev-list --count HEAD", returnStdout: true).trim().toInteger()
                        if (commitCount > 1) {
                            def changedFilesOutput = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                            if (changedFilesOutput) {
                                def changedFiles = changedFilesOutput.split('\n').toList()
                                if (commonModules.any { module -> changedFiles.any { it.startsWith(module + "/") } }) {
                                    echo "-> Common module changed. Adding all services to build list."
                                    changedServices.addAll(allServices)
                                } else {
                                    allServices.each { service ->
                                        if (changedFiles.any { it.startsWith(service + "/") }) {
                                            echo "-> Code changes detected for '${service}'. Adding to build list."
                                            changedServices.add(service)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "\nNo services to build."
                        env.CHANGED_SERVICES = ""
                        currentBuild.result = 'SUCCESS'
                    } else {
                        env.CHANGED_SERVICES = changedServices.unique().join(",")
                    }
                    echo "\n>>> Final services to be built: ${env.CHANGED_SERVICES}"
                }
            }
        }

//         stage('Build & Push Changed Services in Parallel') {
//             when {
//                 expression { env.CHANGED_SERVICES }
//             }
//             steps {
//                 withAWS(region: "${REGION}", credentials: "aws-key") {
//                     script {
//                         def changedServices = (env.CHANGED_SERVICES ?: '').split(",").toList()
//                         def parallelTasks = [:]
//
//                         // ✨ 디버깅: 명령어 실행 과정 추적
//                         sh """
//                           set -x
//                           aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL}
//                         """
//
//                         changedServices.each { service ->
//                             parallelTasks["Build & Push ${service}"] = {
//                                 sh """
//                                     set -x # ✨ 디버깅: 모든 셸 명령어 로그에 출력
//                                     echo "--- Building ${service} ---"
//                                     cd ${service}
//                                     ./gradlew clean build -x test
//                                     cd ..
//                                     echo "--- Building and Pushing Docker image for ${service} ---"
//                                     docker build -t ${service}:latest ./${service}
//                                     docker tag ${service}:latest ${ECR_URL}/${service}:latest
//                                     docker push ${ECR_URL}/${service}:latest
//                                 """
//                             }
//                         }
//                         parallel parallelTasks
//                     }
//                 }
//             }
//         }
    }
}