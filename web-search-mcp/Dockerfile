# Build stage
FROM eclipse-temurin:24-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY common common
COPY devplay-info-mcp/pom.xml devplay-info-mcp/pom.xml
COPY rag-search-mcp/pom.xml rag-search-mcp/pom.xml
COPY chat-history-mcp/pom.xml chat-history-mcp/pom.xml
COPY web-search-mcp web-search-mcp

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl web-search-mcp -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /workspace/web-search-mcp/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
