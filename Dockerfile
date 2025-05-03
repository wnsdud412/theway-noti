FROM eclipse-temurin:21-jdk

# Set the timezone environment variable
ENV TZ=Asia/Seoul

# Install tzdata and set the localtime
RUN apt-get update && \
    apt-get install -y tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 작업 디렉터리 설정
WORKDIR /app

# 컨테이너에서 실행할 명령어
CMD ["java", "-jar", "app.jar"]