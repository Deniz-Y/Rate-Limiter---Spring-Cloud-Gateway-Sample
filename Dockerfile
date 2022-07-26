FROM openjdk:17.0.2-jdk

VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} spring-boot-app.jar

ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar","/spring-boot-app.jar", \
            "-XX:UseSSE", "2"]
EXPOSE $SERVER_PORT $MANAGEMENT_SERVER_PORT