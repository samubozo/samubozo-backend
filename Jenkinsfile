// 자주 사용되는 필요한 변수를 전역으로 선언하는 것도 가능.
def ecrLoginHelper = "docker-credential-ecr-login" // ECR credential helper 이름
def deployHost = "172.31.9.208" // 배포 인스턴스의 private 주소

// 젠킨스의 선언형 파이프라인 정의부 시작 (그루비 언어)
pipeline {
    agent any // 어느 젠킨스 서버에서나 실행이 가능
    environment {
        SERVICE_DIRS = "approval-service,attendance-service,auth-service,certificate-service,chatbot-service,config-service,discovery-service,gateway-service,hr-service,message-service,notification-service,payroll-service,schedule-service,vacation-service"
        ECR_URL = "886331869898.dkr.ecr.ap-northeast-2.amazonaws.com"
        REGION = "ap-northeast-2"
        CHANGED_SERVICES = "" // 초기값 설정
    }
    stages {
        stage('Pull Codes from Github') { // 스테이지 제목 (맘대로 써도 됨)
            steps {
                checkout scm // 젠킨스와 연결된 소스 컨트롤 매니저(git 등)에서 코드를 가져오는 명령어
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
                    def allServices = env.SERVICE_DIRS.split(",")
                    def changedServices = []
                    def shouldBuildAll = false

                    // ECR에 이미지가 존재하는지 확인
                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        try {
                            // ECR 리포지토리 목록을 가져와서 비어있는지 확인
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
                        // Git 커밋 기반 변경 감지 로직
                        def commitCount = sh(script: "git rev-list --count HEAD", returnStdout: true).trim().toInteger()
                        if (commitCount == 1) {
                            echo "Initial commit detected. All services will be built."
                            changedServices = allServices
                        } else {
                            def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true).trim().split('\n')
                            echo "Changed files: ${changedFiles}"

                            allServices.each { service ->
                                if (changedFiles.any { it.startsWith(service + "/") }) {
                                    changedServices.add(service)
                                }
                            }
                        }
                    }

                    env.CHANGED_SERVICES = changedServices.join(",")
                    if (env.CHANGED_SERVICES == "") {
                        echo "No changes detected in service directories. Skipping build and deployment."
                        currentBuild.result = 'SUCCESS'
                    }
                }
            }
        }

        stage('Build Changed Services') {
            when {
                expression { env.CHANGED_SERVICES != "" }
            }
            steps {
                script {
                   def changedServices = env.CHANGED_SERVICES.split(",")
                   changedServices.each { service ->
                        sh """
                        echo "Building ${service}..."
                        cd ${service}
                        ./gradlew clean build -x test
                        ls -al ./build/libs
                        cd ..
                        """
                   }
                }
            }
        }

        stage('Build Docker Image & Push to AWS ECR') {
            when {
                expression { env.CHANGED_SERVICES != "" }
            }
            steps {
                script {
                    withAWS(region: "${REGION}", credentials: "jenkins-ssh-key") {
                        def changedServices = env.CHANGED_SERVICES.split(",")
                        changedServices.each { service ->
                            sh """
                            # ECR에 이미지를 push하기 위해 인증 정보를 대신 검증해 주는 도구 다운로드.
                            # /usr/local/bin/ 경로에 해당 파일을 이동
                            curl -O https://amazon-ecr-credential-helper-releases.s3.us-east-2.amazonaws.com/0.4.0/linux-amd64/${ecrLoginHelper}
                            chmod +x ${ecrLoginHelper}
                            mv ${ecrLoginHelper} /usr/local/bin/

                            # Docker에게 push 명령을 내리면 지정된 URL로 push할 수 있게 설정.
                            # 자동으로 로그인 도구를 쓰게 설정
                            mkdir -p ~/.docker
                            echo '{"credHelpers": {"${ECR_URL}": "ecr-login"}}' > ~/.docker/config.json

                            docker build -t ${service}:latest ${service}
                            docker tag ${service}:latest ${ECR_URL}/${service}:latest
                            docker push ${ECR_URL}/${service}:latest
                            """
                        }
                    }
                }
            }
        }

//         stage('Deploy Changed Services to AWS EC2') {
//             steps {
//                 sshagent(credentials: ["deploy-key"]) {
//                     sh """
//                         echo "[INFO] SCP docker-compose.yml 전송 중..."
//                         scp -vvv -o StrictHostKeyChecking=no docker-compose.yml ubuntu@${deployHost}:/home/ubuntu/docker-compose.yml
//
//                         echo "[INFO] SSH 접속 및 배포 실행..."
//                         ssh -v -o StrictHostKeyChecking=no ubuntu@${deployHost} '
//                             set -e  # 에러 발생 시 즉시 종료
//                             cd /home/ubuntu && \\
//                             echo "[INFO] ECR 로그인 중..." && \\
//                             aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL} && \\
//                             echo "[INFO] 이미지 Pull 중: ${env.CHANGED_SERVICES}" && \\
//                             docker-compose pull ${env.CHANGED_SERVICES.replace(",", " ")} && \\
//                             echo "[INFO] 서비스 재시작 중..." && \\
//                             docker-compose up -d ${env.CHANGED_SERVICES.replace(",", " ")}
//                         '
//                     """
//                 }
//             }
//         }


    }
}