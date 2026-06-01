# Build stage
FROM eclipse-temurin:24-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY devplay-info-mcp/pom.xml devplay-info-mcp/pom.xml
COPY rag-search-mcp/pom.xml rag-search-mcp/pom.xml
COPY web-search-mcp/pom.xml web-search-mcp/pom.xml
COPY common common
COPY chat-history-mcp chat-history-mcp

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -pl chat-history-mcp -am package -DskipTests

# Runtime stage
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /workspace/chat-history-mcp/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
