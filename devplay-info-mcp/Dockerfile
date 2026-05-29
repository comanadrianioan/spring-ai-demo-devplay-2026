# Build stage
FROM eclipse-temurin:24-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY common common
COPY rag-search-mcp/pom.xml rag-search-mcp/pom.xml
COPY chat-history-mcp/pom.xml chat-history-mcp/pom.xml
COPY web-search-mcp/pom.xml web-search-mcp/pom.xml
COPY devplay-info-mcp devplay-info-mcp

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl devplay-info-mcp -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /workspace/devplay-info-mcp/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
