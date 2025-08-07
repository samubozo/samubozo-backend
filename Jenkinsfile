// ... pipeline의 다른 부분은 그대로 유지 ...

    stages {
        // ... 'Detect Changes' 스테이지까지는 동일 ...
        stage('Detect Changes') {
            // ... (이전과 동일) ...
        }

        // 🚨 수정: 'ECR 로그인 준비' 단계를 별도 stage로 분리
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

        // 🚨 수정: 병렬 빌드 & 푸시를 위한 stage 구조 수정
        stage('Build & Push Changed Services in Parallel') {
            when {
                expression { env.CHANGED_SERVICES != "" }
            }
            steps {
                // script 블록을 steps 아래로 이동
                script {
                    def changedServices = env.CHANGED_SERVICES.split(",")
                    // 병렬로 실행할 작업들을 맵(Map) 형태로 정의
                    def parallelTasks = [:]

                    changedServices.each { service ->
                        // 각 서비스에 대한 빌드/푸시 작업을 정의
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
                    // 정의된 작업들을 병렬로 실행
                    parallel parallelTasks
                }
            }
        }

        stage('Deploy Changed Services to AWS EC2') {
            // ... (이전과 동일) ...
        }
    }
}