# Build stage
FROM eclipse-temurin:24-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY devplay-info-mcp/pom.xml devplay-info-mcp/pom.xml
COPY chat-history-mcp/pom.xml chat-history-mcp/pom.xml
COPY web-search-mcp/pom.xml web-search-mcp/pom.xml
COPY common common
COPY rag-search-mcp rag-search-mcp

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl rag-search-mcp -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /workspace/rag-search-mcp/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
