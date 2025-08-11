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
                    def changedServices = []
                    def previousCommit = getPreviousCommit()

                    echo "\n🔍 Starting Git changes check..."

                    // Git 변경사항만 체크
                    // 수정: 마지막 성공 빌드와 현재 빌드 커밋을 비교하도록 변경
                    changedServices = checkGitChanges(allServices, previousCommit)

                    // 최종 결과 처리
                    if (changedServices) {
                        def uniqueServices = changedServices.unique()
                        GLOBAL_CHANGED_SERVICES = uniqueServices.join(",")
                    }

                    // 결과 출력
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
                        echo "✅ FINAL RESULT: No services to build"
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
                    def newTag = env.GIT_COMMIT
                    def servicesToBuild = GLOBAL_CHANGED_SERVICES.split(",").toList()
                    echo "🔨 Building ${servicesToBuild.size()} services sequentially..."

                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        // ECR 로그인
                        sh """
                            aws ecr get-login-password --region ${REGION} | \\
                            docker login --username AWS --password-stdin ${ECR_URL}
                        """

                        // 순차적으로 빌드
                        servicesToBuild.each { service ->
                            try {
                                echo "\n📦 Building ${service}..."

                                // Docker 이미지 빌드 및 푸시
                                sh """
                                    docker build --platform linux/amd64 -t ${service}:${newTag} ${service}
                                    docker tag ${service}:${newTag} ${ECR_URL}/${service}:${newTag}
                                    docker push ${ECR_URL}/${service}:${newTag}
                                """

                                echo "✅ ${service} completed"

                                // 메모리 정리를 위한 개별 Docker 이미지 삭제
                                sh """
                                    docker rmi ${service}:${newTag} || true
                                    docker rmi ${ECR_URL}/${service}:${newTag} || true
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

        stage('Update K8s Repo') {
            when {
                expression {
                    return GLOBAL_CHANGED_SERVICES != null && GLOBAL_CHANGED_SERVICES != ""
                }
            }
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: "git-login-info", usernameVariable: "GIT_USERNAME", passwordVariable: 'GIT_PASSWORD')]) {
                        echo "========================================="
                        echo "     Updating K8s Git Repo Stage Starting"
                        echo "========================================="
                        sh '''
                            cd ..
                            if [ -d "samubozo-backend" ]; then
                                echo "Deleting existing samubozo-backend directory..."
                                rm -rf samubozo-backend
                            fi
                            git clone -b ingressTest https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/samubozo/samubozo-backend.git
                        '''

                        def servicesToUpdate = GLOBAL_CHANGED_SERVICES.split(",")
                        def newTag = env.GIT_COMMIT

                        servicesToUpdate.each { service ->
                          echo "Updating image tag for ${service} to ${newTag}"
                          sh """
                            cd ../samubozo-backend
                            sed -E -i 's|(image:\\s*${ECR_URL}/${service}:)[^[:space:]]+|\\1${newTag}|' \
                              ./deploy/msa-chart/charts/${service}/values.yaml
                          """
                        }

                        sh '''
                            cd ../samubozo-backend
                            git config user.name "Jenkins"
                            git config user.email "jenkins@example.com"
                            git add .
                            git commit -m "Update images for services: ${GLOBAL_CHANGED_SERVICES}"
                            git push origin ingressTest
                        '''
                        echo "✅ K8s Git repo updated successfully!"
                    }
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
            echo "🧹 Cleaning up..."

            // 사용하지 않는 모든 도커 리소스 (이미지, 컨테이너, 네트워크 등) 정리
            sh 'docker system prune -af'

            // Jenkins 워크스페이스 정리
            deleteDir()
            echo "✅ Cleanup finished."
        }
    }
}

// ======================================================
// Helper Functions (pipeline 블록 밖에 정의)
// ======================================================

def checkGitChanges(serviceList, previousCommit) {
    def changedServices = []

    try {
        // 커밋 수 확인
        def commitCount = sh(
            script: "git rev-list --count HEAD",
            returnStdout: true
        ).trim().toInteger()

        // 첫 번째 빌드인 경우
        if (commitCount <= 1 || previousCommit == null) {
            echo "  First commit or no previous successful build detected - will build all services."
            return serviceList
        }

        // 마지막 성공 빌드와 현재 빌드의 커밋을 비교
        def changedFiles = sh(
            script: "git diff --name-only ${previousCommit} HEAD",
            returnStdout: true
        ).trim()

        if (!changedFiles) {
            echo "  No files changed since the last successful build."
            return changedServices
        }

        echo "  Changed files detected since commit ${previousCommit}:"
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
        // 에러 발생 시 모든 서비스를 빌드하도록 기본 처리
        return serviceList
    }

    return changedServices
}

// 수정: 이전 성공 빌드의 커밋 정보를 가져오는 헬퍼 함수 추가
def getPreviousCommit() {
    def previousCommit = null
    def previousBuild = currentBuild.getPreviousSuccessfulBuild()
    if (previousBuild) {
        def previousBuildCause = previousBuild.getCauses().find { it instanceof hudson.model.Cause.UserIdCause }
        if (previousBuildCause) {
            // 이전에 성공한 빌드의 커밋 해시를 가져옴
            previousCommit = previousBuild.getBuildByNumber(previousBuild.getNumber()).getChangeSet().getRevisions().first()?.getHash()
            if (!previousCommit) {
                // ChangeSet이 없는 경우, SCM 정보에서 가져옴
                def lastScmRevision = previousBuild.getAction(hudson.scm.RevisionParameterAction.class)?.getRevisions()?.first()
                if (lastScmRevision) {
                    previousCommit = lastScmRevision.hash
                }
            }
        }
    }
    return previousCommit
}