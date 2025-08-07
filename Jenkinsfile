// ======================================================
// 최종 Jenkinsfile (각 서비스별 ECR 이미지 감지 로직 적용)
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

                    // --- 1. ECR에 이미지가 없는 서비스를 먼저 찾습니다. ---
                    echo "--- Checking for services with no images in ECR ---"
                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        allServices.each { service ->
                            try {
                                // 특정 리포지토리의 이미지 정보를 요청. 이미지가 없거나 리포지토리가 없으면 실패합니다.
                                // stdout과 stderr를 모두 /dev/null로 보내 출력을 숨깁니다.
                                sh "aws ecr describe-images --repository-name ${service} --max-items 1 > /dev/null 2>&1"
                            } catch (Exception e) {
                                // 명령어 실패 시 (이미지 없음), 빌드 목록에 추가합니다.
                                echo "-> No images found for '${service}' in ECR. Adding to build list."
                                changedServices.add(service)
                            }
                        }
                    }

                    // --- 2. Git 변경 사항을 감지하여 빌드 목록에 추가합니다. ---
                    echo "\n--- Checking for services with code changes via Git ---"
                    def commitCount = sh(script: "git rev-list --count HEAD", returnStdout: true).trim().toInteger()

                    // 첫 커밋이 아닐 경우에만 diff를 확인 (첫 커밋은 위 ECR 체크에서 모두 걸러짐)
                    if (commitCount > 1) {
                        def changedFilesOutput = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                        if (changedFilesOutput) { // 변경된 파일이 있을 경우
                            def changedFiles = changedFilesOutput.split('\n').toList()
                            echo "Changed files: ${changedFiles}"

                            // 공통 모듈 변경 시 전체 빌드
                            if (commonModules.any { module -> changedFiles.any { it.startsWith(module + "/") } }) {
                                echo "-> Common module changed. Adding all services to build list."
                                changedServices.addAll(allServices)
                            } else {
                                // 각 서비스별 코드 변경 확인
                                allServices.each { service ->
                                    if (changedFiles.any { it.startsWith(service + "/") }) {
                                        echo "-> Code changes detected for '${service}'. Adding to build list."
                                        changedServices.add(service)
                                    }
                                }
                            }
                        } else {
                            echo "No code changes detected in this commit."
                        }
                    } else {
                        echo "Initial commit. The ECR check will determine the build list."
                    }

                    // --- 3. 최종 빌드 목록을 확정합니다. ---
                    if (changedServices.isEmpty()) {
                        echo "\nNo services to build."
                        env.CHANGED_SERVICES = ""
                        currentBuild.result = 'SUCCESS'
                    } else {
                        // 중복을 제거하여 최종 목록 생성
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
//                         sh "aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL}"
//
//                         changedServices.each { service ->
//                             parallelTasks["Build & Push ${service}"] = {
//                                 sh """
//                                     echo "--- Building ${service} ---"
//                                     cd ${service}
//                                     ./gradlew clean build -x test
//                                     cd ..
//
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