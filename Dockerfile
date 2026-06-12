FROM docker.io/library/eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN ./mvnw -DskipTests package && \
mv target/*.jar app.jar

EXPOSE 8080 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
