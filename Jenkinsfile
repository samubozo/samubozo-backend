// ======================================================
// Jenkinsfile - Dockerfile 빌드 위임 버전
// ======================================================

// 전역 변수 선언 (pipeline 블록 밖에 선언)
def GLOBAL_CHANGED_SERVICES = ""

pipeline {
    agent any

    environment {
        SERVICE_DIRS = "approval-service,attendance-service,auth-service,certificate-service,chatbot-service,config-service,gateway-service,hr-service,message-service,notification-service,payroll-service,schedule-service,vacation-service"
        ECR_URL = "886331869898.dkr.ecr.ap-northeast-2.amazonaws.com"
        REGION = "ap-northeast-2"
        COMMON_MODULES = "common-module,parent-module"
        EKS_CLUSTER_NAME = "samubozo-eks"
    }

    stages {
        stage('Initial Setup') {
            steps {
                echo "========================================="
                echo "     Initial Setup Stage Starting"
                echo "========================================="

                deleteDir()
                checkout scm

                withCredentials([file(credentialsId: 'config-secret', variable: 'configSecret')]) {
                    sh 'cp $configSecret config-service/src/main/resources/application-dev.yml'
                }

                echo "✅ Initial setup completed"
            }
        }

        stage('Detect Changes') {
            steps {
                script {
                    echo "========================================="
                    echo "     Change Detection Stage Starting"
                    echo "========================================="

                    def allServices = env.SERVICE_DIRS.split(",").toList()
                    def detectedServices = []

                    echo "\n🔍 Starting automatic change detection..."

                    // 1. ECR 빈 레포지토리 체크
                    echo "\n--- Phase 1: ECR Repository Check ---"
                    def ecrEmptyServices = checkECRRepositories(allServices)

                    if (ecrEmptyServices) {
                        echo "📦 ECR empty services found: ${ecrEmptyServices.size()} services"
                        echo "   Services: ${ecrEmptyServices.join(', ')}"
                        detectedServices.addAll(ecrEmptyServices)
                        echo "⚡ Skipping Git check - ECR rebuild required"
                    } else {
                        echo "✅ All services have images in ECR"

                        // 2. Git 변경사항 체크 (ECR이 모두 차있을 때만)
                        echo "\n--- Phase 2: Git Changes Check ---"
                        def gitChangedServices = checkGitChanges(allServices)

                        if (gitChangedServices) {
                            echo "📝 Git changes found: ${gitChangedServices.size()} services"
                            echo "   Services: ${gitChangedServices.join(', ')}"
                            detectedServices.addAll(gitChangedServices)
                        } else {
                            echo "✅ No Git changes detected"
                        }
                    }

                    // 3. 최종 결과 처리
                    if (detectedServices) {
                        def uniqueServices = detectedServices.unique()
                        GLOBAL_CHANGED_SERVICES = uniqueServices.join(",")
                    }

                    // 4. 결과 출력
                    echo "\n========================================="
                    if (GLOBAL_CHANGED_SERVICES) {
                        def serviceList = GLOBAL_CHANGED_SERVICES.split(",")
                        echo "🎯 FINAL RESULT: ${serviceList.size()} services to build"
                        echo "========================================="
                        echo "Services to build:"
                        serviceList.each { service ->
                            echo "  • ${service}"
                        }
                        currentBuild.description = "Building ${serviceList.size()} services"
                    } else {
                        echo "⚠️  FINAL RESULT: No services to build"
                        echo "========================================="
                        currentBuild.description = "No changes detected"
                        currentBuild.result = 'SUCCESS'
                    }
                }
            }
        }
        stage('Build & Push Services - Sequential') {
            when {
                expression {
                    return GLOBAL_CHANGED_SERVICES != null && GLOBAL_CHANGED_SERVICES != ""
                }
            }
            steps {
                script {
                    echo "========================================="
                    echo "     Build & Push Stage Starting"
                    echo "========================================="

                    def servicesToBuild = GLOBAL_CHANGED_SERVICES.split(",").toList()
                    echo "🔨 Building ${servicesToBuild.size()} services sequentially..."

                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        // ECR 로그인
                        sh """
                            aws ecr get-login-password --region ${REGION} | \
                            docker login --username AWS --password-stdin ${ECR_URL}
                        """

                        // 순차적으로 빌드
                        servicesToBuild.each { service ->
                            try {
                                echo "\n📦 Building ${service}..."

                                // Docker 이미지 빌드 및 푸시
                                sh """
                                    docker build -t ${service}:latest ./${service}
                                    docker tag ${service}:latest ${ECR_URL}/${service}:latest
                                    docker push ${ECR_URL}/${service}:latest
                                """

                                echo "✅ ${service} completed"

                                // 메모리 정리를 위한 Docker 이미지 삭제
                                sh """
                                    docker rmi ${service}:latest || true
                                    docker rmi ${ECR_URL}/${service}:latest || true
                                """

                            } catch (Exception e) {
                                echo "❌ ${service} failed: ${e.message}"
                                throw e
                            }
                        }
                    }

                    echo "\n✅ All services built and pushed successfully!"
                }
            }
        }

        stage('Deploy Services to EKS') {
            when {
                expression {
                    return GLOBAL_CHANGED_SERVICES != null && GLOBAL_CHANGED_SERVICES != ""
                }
            }
            steps {
                script {
                    echo "========================================="
                    echo "     Deploy Services Stage Starting"
                    echo "========================================="

                    def changedServicesString = GLOBAL_CHANGED_SERVICES.split(",").join(",")
                    echo "🎯 Deploying services: ${changedServicesString}"

                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        // EKS 클러스터 인증 정보 업데이트
                        sh """
                            aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME} --region ${REGION}
                        """

                        try {
                            echo "\n🚀 Deploying msa-chart to EKS using Helm..."

                            sh """
                                helm upgrade --install msa-app ./deploy/msa-chart \\
                                    --set global.ecrUrl=${ECR_URL} \\
                                    --set global.services=${changedServicesString} \\
                                    --set global.image.tag=latest
                            """

                            echo "✅ msa-chart deployment completed"

                        } catch (Exception e) {
                            echo "❌ msa-chart deployment failed: ${e.message}"
                            throw e
                        }
                    }
                    echo "\n✅ All services deployed successfully!"
                }
            }
        }
    }

    post {
        success {
            script {
                echo "✅ Pipeline completed successfully!"
                if (GLOBAL_CHANGED_SERVICES) {
                    echo "Built services: ${GLOBAL_CHANGED_SERVICES}"
                }
            }
        }
        failure {
            echo "❌ Pipeline failed!"
        }
        always {
            echo "🧹 Cleaning up workspace..."
            deleteDir()
        }
    }
}

// ======================================================
// Helper Functions (pipeline 블록 밖에 정의)
// ======================================================

def checkECRRepositories(serviceList) {
    def emptyServices = []

    withAWS(region: "${env.REGION}", credentials: "aws-key") {
        serviceList.each { service ->
            echo "  Checking: ${service}"

            try {
                // 이미지 리스트 확인
                def imageCheck = sh(
                    script: """
                        aws ecr list-images \
                            --repository-name ${service} \
                            --query 'imageIds[0]' \
                            --output text 2>&1
                    """,
                    returnStdout: true
                ).trim()

                if (imageCheck == "None" || imageCheck == "") {
                    echo "    ↳ ❌ No images found"
                    emptyServices.add(service)
                } else {
                    echo "    ↳ ✅ Images exist"
                }

            } catch (Exception e) {
                // 레포지토리가 없는 경우
                echo "    ↳ ❌ Repository not found"
                emptyServices.add(service)
            }
        }
    }

    return emptyServices
}

def checkGitChanges(serviceList) {
    def changedServices = []

    try {
        // 커밋 수 확인
        def commitCount = sh(
            script: "git rev-list --count HEAD",
            returnStdout: true
        ).trim().toInteger()

        if (commitCount <= 1) {
            echo "  First commit detected - skipping Git change detection"
            return changedServices
        }

        // 변경된 파일 목록 가져오기
        def changedFiles = sh(
            script: "git diff --name-only HEAD~1 HEAD",
            returnStdout: true
        ).trim()

        if (!changedFiles) {
            echo "  No files changed in last commit"
            return changedServices
        }

        echo "  Changed files detected:"
        changedFiles.split('\n').each { file ->
            echo "    • ${file}"
        }

        // 변경된 파일 분석
        def fileList = changedFiles.split('\n').toList()
        def commonModules = env.COMMON_MODULES.split(",").toList()

        // 공통 모듈 변경 체크
        def commonChanged = commonModules.any { module ->
            fileList.any { file -> file.startsWith("${module}/") }
        }

        if (commonChanged) {
            echo "  ⚠️  Common module changed - all services will be rebuilt"
            return serviceList
        }

        // 개별 서비스 변경 체크
        serviceList.each { service ->
            if (fileList.any { file -> file.startsWith("${service}/") }) {
                changedServices.add(service)
            }
        }

    } catch (Exception e) {
        echo "  ⚠️  Error during Git change detection: ${e.message}"
    }

    return changedServices
}