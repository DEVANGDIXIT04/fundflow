# ---------- Stage 1: build ----------
# Full JDK + Maven wrapper. Everything here (Maven distro, dependency cache,
# compiler) is thrown away; only the extracted app moves to the runtime stage.
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# Copy just the build descriptors first: as long as pom.xml is unchanged,
# Docker reuses the cached dependency layer and source edits rebuild in seconds.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests && \
    java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ---------- Stage 2: runtime ----------
# JRE-only Alpine image: no compiler, no Maven, no source — smaller surface
# for both image size and security.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Never run as root: a container escape from a non-root process is far less useful.
RUN addgroup -S app && adduser -S app -G app
USER app

# Spring Boot layered-jar extraction, copied least-changing first so image
# rebuilds only ship the layers that actually changed (deps ~50MB vs app ~50KB).
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
