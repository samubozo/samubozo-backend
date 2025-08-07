// Jenkinsfile 전체 코드 (수정 완료)

// 자주 사용되는 필요한 변수를 전역으로 선언
def deployHost = "172.31.9.208" // 배포 인스턴스의 private 주소

// 젠킨스의 선언형 파이프라인 정의부 시작 (그루비 언어)
pipeline {
    agent any // 어느 젠킨스 서버에서나 실행이 가능
    environment {
        SERVICE_DIRS = "approval-service,attendance-service,auth-service,certificate-service,chatbot-service,config-service,discovery-service,gateway-service,hr-service,message-service,notification-service,payroll-service,schedule-service,vacation-service"
        ECR_URL = "886331869898.dkr.ecr.ap-northeast-2.amazonaws.com"
        REGION = "ap-northeast-2"
        CHANGED_SERVICES = "" // 초기값 설정
        COMMON_MODULES = "common-module,parent-module" // 공통 모듈 (프로젝트에 맞게 수정)
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
                    // 🚨 수정: .split() 결과를 .toList()로 변환하여 배열이 아닌 리스트로 만듭니다.
                    def allServices = env.SERVICE_DIRS.split(",").toList()
                    def commonModules = env.COMMON_MODULES.split(",").toList()
                    def changedServices = []
                    def shouldBuildAll = false

                    // ECR 저장소 존재 여부로 최초 빌드인지 확인
                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        try {
                            def repoListJson = sh(script: "aws ecr describe-repositories --output json", returnStdout: true)
                            def repoList = new groovy.json.JsonSlurper().parseText(repoListJson)
                            if (repoList.repositories.isEmpty()) {
                                echo "No ECR repositories found. Building all services for the first time."
                                shouldBuildAll = true
                            }
                        } catch (Exception e) {
                            echo "Failed to check ECR repositories: ${e.getMessage()}. Assuming first build."
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
                                echo "No changes detected between HEAD~1 and HEAD."
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

                    // 이제 changedServices는 항상 리스트 타입이므로 .isEmpty() 호출이 안전합니다.
                    if (changedServices.isEmpty()) {
                        echo "No changes detected in service directories. Skipping build and deployment."
                        env.CHANGED_SERVICES = ""
                        currentBuild.result = 'SUCCESS'
                    } else {
                         env.CHANGED_SERVICES = changedServices.unique().join(",")
                    }

                    echo "Services to be built: ${env.CHANGED_SERVICES}"
                }
            }
        }

        stage('Prepare ECR Login') {
            when {
                expression { env.CHANGED_SERVICES != "" }
            }
            steps {
                withAWS(region: "${REGION}", credentials: "aws-key") {
                     sh """
                        echo "Setting up Docker for ECR Push..."
                        if [ ! -f /usr/local/bin/docker-credential-ecr-login ]; then
                            curl -sL -o docker-credential-ecr-login https://amazon-ecr-credential-helper-releases.s3.us-east-2.amazonaws.com/0.6.0/linux-amd64/docker-credential-ecr-login
                            chmod +x docker-credential-ecr-login
                            sudo mv docker-credential-ecr-login /usr/local/bin/
                        fi
                        mkdir -p ~/.docker
                        echo '{"credHelpers": {"${ECR_URL}": "ecr-login"}}' > ~/.docker/config.json
                    """
                }
            }
        }

        stage('Build & Push Changed Services in Parallel') {
            when {
                expression { env.CHANGED_SERVICES != "" }
            }
            steps {
                script {
                    def changedServices = env.CHANGED_SERVICES.split(",").toList()
                    def parallelTasks = [:]

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

        stage('Deploy Changed Services to AWS EC2') {
            when {
                expression { env.CHANGED_SERVICES != "" }
            }
            steps {
                sshagent(credentials: ["deploy-key"]) {
                    sh """
                        echo "[INFO] SCP docker-compose.yml 전송 중..."
                        scp -o StrictHostKeyChecking=no docker-compose.yml ubuntu@${deployHost}:/home/ubuntu/docker-compose.yml

                        echo "[INFO] SSH 접속 및 배포 실행..."
                        ssh -o StrictHostKeyChecking=no ubuntu@${deployHost} '
                            set -e  # 에러 발생 시 즉시 종료

                            echo "[INFO] ECR 로그인 중..."
                            aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL}

                            cd /home/ubuntu

                            CHANGED_SERVICES_FOR_COMPOSE="${env.CHANGED_SERVICES.replace(",", " ")}"

                            echo "[INFO] 이미지 Pull 중: ${CHANGED_SERVICES_FOR_COMPOSE}"
                            docker-compose pull ${CHANGED_SERVICES_FOR_COMPOSE}

                            echo "[INFO] 서비스 재시작 중..."
                            docker-compose up -d --remove-orphans ${CHANGED_SERVICES_FOR_COMPOSE}
                        '
                    """
                }
            }
        }
    }
}