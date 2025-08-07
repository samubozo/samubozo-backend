// ======================================================
// 최종 디버깅용 Jenkinsfile (AWS CLI 결과 원본 출력)
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
                    def changedServices = []

                    if (params.MANUAL_BUILD_SERVICES.trim()) {
                        echo "Manual build triggered for: ${params.MANUAL_BUILD_SERVICES}"
                        changedServices = params.MANUAL_BUILD_SERVICES.split(',').collect { it.trim() }

                    } else {
                        echo "--- Checking for services with no images in ECR ---"
                        withAWS(region: "${REGION}", credentials: "aws-key") {
                            allServices.each { service ->
                                try {
                                    echo "\n--- DEBUGGING ECR FOR: ${service} ---"
                                    // ✨✨✨ 핵심: 반환된 JSON 결과를 로그에 그대로 출력합니다 ✨✨✨
                                    def imageJson = sh(script: "aws ecr describe-images --repository-name ${service}", returnStdout: true).trim()
                                    echo "RAW JSON for ${service}:\n${imageJson}"

                                    def imageInfo = new groovy.json.JsonSlurper().parseText(imageJson)

                                    if (imageInfo.imageDetails.isEmpty()) {
                                        echo "-> RESULT: Parsed as EMPTY. Adding to build list."
                                        changedServices.add(service)
                                    } else {
                                        echo "-> RESULT: Parsed as NOT EMPTY. Skipping."
                                    }
                                } catch (Exception e) {
                                    echo "-> COMMAND FAILED for ${service} with exception: ${e.getMessage()}"
                                    echo "-> Adding to build list."
                                    changedServices.add(service)
                                }
                            }
                        }
                    }

                    // 최종 빌드 목록 확정
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

//         // 이하 스테이지는 동일
//         stage('Build & Push Changed Services in Parallel') {
//             when {
//                 expression { env.CHANGED_SERVICES }
//             }
//             steps {
//                 withAWS(region: "${REGION}", credentials: "aws-key") {
//                     script {
//                         def changedServices = (env.CHANGED_SERVICES ?: '').split(",").toList()
//                         def parallelTasks = [:]
//                         sh "aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL}"
//                         changedServices.each { service ->
//                             parallelTasks["Build & Push ${service}"] = {
//                                 sh """
//                                     echo "--- Building ${service} ---"
//                                     cd ${service}
//                                     ./gradlew clean build -x test
//                                     cd ..
//                                     echo "--- Building and Pushing Docker image for ${service} ---"
//                                     docker build -t ${service}:latest ./${service}
//                                     docker tag ${service}:latest ${ECR_URL}/${service}:latest
//                                     docker push ${service}:latest
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