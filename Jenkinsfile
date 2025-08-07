// ======================================================
// 최종 완성형 Jenkinsfile (파일 시스템을 이용한 버그 우회 - 수정본)
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
                // 이전 빌드의 임시 파일이 남아있을 수 있으므로 정리
                deleteDir()
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
                    def finalChangedServices = []

                    if (params.MANUAL_BUILD_SERVICES.trim()) {
                        echo "Manual build triggered for: ${params.MANUAL_BUILD_SERVICES}"
                        finalChangedServices = params.MANUAL_BUILD_SERVICES.split(',').collect { it.trim() }

                    } else {
                        // ✨ 수정: ECR 체크 결과를 직접 리스트에 저장
                        echo "--- Checking for services with no images in ECR ---"
                        def ecrEmptyServices = []

                        withAWS(region: "${REGION}", credentials: "aws-key") {
                            allServices.each { service ->
                                echo "Checking ECR for service: ${service}"

                                // 방법 2: list-images 사용 (더 신뢰할 수 있음)
                                def listOutput = sh(
                                    script: """
                                        aws ecr list-images --repository-name ${service} --query 'imageIds[0]' --output text 2>&1 || echo "ERROR"
                                    """,
                                    returnStdout: true
                                ).trim()

                                echo "List images output for ${service}: ${listOutput}"

                                // 이미지가 없거나 레포지토리가 없는 경우 처리
                                if (listOutput == "None" || listOutput == "" || listOutput.contains("ERROR") || listOutput.contains("RepositoryNotFoundException")) {
                                    echo "-> No images found for '${service}' in ECR. Adding to list."
                                    ecrEmptyServices.add(service)
                                } else {
                                    echo "-> Images exist for '${service}' in ECR."
                                }
                            }
                        }

                        // ECR 결과를 바로 finalChangedServices에 추가
                        if (!ecrEmptyServices.isEmpty()) {
                            echo "ECR empty services found: ${ecrEmptyServices.join(',')}"
                            finalChangedServices.addAll(ecrEmptyServices)
                        }

                        // 2. Git 변경 감지
                        echo "\n--- Checking for services with code changes via Git ---"
                        def gitChangedServices = []
                        def commitCount = sh(script: "git rev-list --count HEAD", returnStdout: true).trim().toInteger()

                        if (commitCount > 1) {
                            def changedFilesOutput = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim()
                            if (changedFilesOutput) {
                                def changedFiles = changedFilesOutput.split('\n').toList()
                                def commonModules = env.COMMON_MODULES.split(",").toList()

                                // 공통 모듈이 변경되면 모든 서비스 빌드
                                if (commonModules.any { module -> changedFiles.any { it.startsWith(module + "/") } }) {
                                    echo "Common module changed, all services will be built"
                                    gitChangedServices.addAll(allServices)
                                } else {
                                    // 개별 서비스 변경 확인
                                    allServices.each { service ->
                                        if (changedFiles.any { it.startsWith(service + "/") }) {
                                            echo "Git changes detected in: ${service}"
                                            gitChangedServices.add(service)
                                        }
                                    }
                                }
                            }
                        } else {
                            echo "This is the first commit, checking all services"
                            // 첫 커밋인 경우 모든 서비스 체크 (ECR 체크만 진행됨)
                        }

                        // Git 변경사항 추가
                        if (!gitChangedServices.isEmpty()) {
                            echo "Git changed services: ${gitChangedServices.join(',')}"
                            finalChangedServices.addAll(gitChangedServices)
                        }
                    }

                    // 중복 제거 및 최종 결과 설정
                    if (finalChangedServices.isEmpty()) {
                        echo "\n⚠️ No services to build."
                        env.CHANGED_SERVICES = ""
                        currentBuild.result = 'SUCCESS'
                    } else {
                        def uniqueServices = finalChangedServices.unique()
                        env.CHANGED_SERVICES = uniqueServices.join(",")
                        echo "\n✅ Final services to be built: ${env.CHANGED_SERVICES}"
                        echo "Total count: ${uniqueServices.size()}"
                    }
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