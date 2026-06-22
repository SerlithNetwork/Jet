# syntax=docker/dockerfile:1

# Referenced from gh.com/lucko/bytebin
FROM eclipse-temurin:25-alpine

RUN addgroup -S jet && adduser -S -G jet jet
USER jet

WORKDIR /opt/jet
COPY build/libs/Jet-0.0.1-SNAPSHOT.jar ./jet.jar

RUN mkdir pictures config
VOLUME ["/opt/jet/pictures", "/opt/jet/config"]

HEALTHCHECK --interval=1m --timeout=5s \
    CMD wget http://localhost:9420/api/v1/health -q -O - | grep -c '{"status":"ok"}' || exit 1

CMD ["java", "-jar", "jet.jar"]
EXPOSE 9420/tcp
