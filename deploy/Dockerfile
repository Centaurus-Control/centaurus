FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build-output/centaurus-server.jar /app/centaurus-server.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/centaurus-server.jar"]

