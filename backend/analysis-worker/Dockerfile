FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/analysis-worker.jar app.jar
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=50.0","-jar","app.jar"]
