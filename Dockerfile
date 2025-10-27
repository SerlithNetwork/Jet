
# Referenced from gh.com/lucko/bytebin
FROM gradle:jdk21-corretto AS build-project

WORKDIR /jet
COPY gradle/ ./gradle/
COPY src/ ./src/
COPY build.gradle.kts ./
COPY gradlew ./
COPY gradlew.bat ./
COPY settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew build --no-daemon --stacktrace

FROM eclipse-temurin:25-alpine

RUN addgroup -S jet && adduser -S -G jet jet
USER jet

WORKDIR /opt/jet
COPY --from=build-project /Jet/build/libs/Jet-0.0.1-SNAPSHOT.jar ./jet.jar

RUN mkdir pictures config
VOLUME ["/opt/jet/pictures", "/opt/jet/config"]

HEALTHCHECK --interval=1m --timeout=5s \
    CMD wget http://localhost:9420/api/health -q -O - | grep -c '{"status":"ok"}' || exit 1

CMD ["java", "-jar", "jet.jar"]
EXPOSE 9420/tcp
EXPOSE 9430/tcp
