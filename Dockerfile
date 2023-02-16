FROM gradle:jdk17-alpine as builder
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN apk update && \
    apk add --no-cache git && \
    ls && \
    git config --global --add safe.directory /home/gradle/src && \
    ./gradlew cleanBuild idea remapSpigotJar --no-daemon -i --stacktrace --refresh-dependencies && \
    ./gradlew build --no-daemon -i --stacktrace && \
    ./gradlew build collect --no-daemon -i --stacktrace

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /home/gradle/src/build/libs/*.jar /app/server.jar

RUN echo "eula=true" > /app/eula.txt

ENTRYPOINT ["java", "-jar", "server.jar", "nogui"]
