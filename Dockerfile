# syntax=docker/dockerfile:1

# ---- Build stage ----------------------------------------------------------
# davos targets Java 8 (sourceCompatibility 1.8) and ships a Gradle 2.14
# wrapper, so the build must run on a JDK 8.
FROM eclipse-temurin:8-jdk AS build

WORKDIR /src

# Copy the wrapper first so the Gradle distribution can be cached between
# builds when only sources change.
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
RUN chmod +x gradlew

# Copy the rest of the project.
COPY . .

# Build the executable Spring Boot jar using the "release" configuration
# (writes its H2 database and logs under /config inside the container).
# Tests are skipped here; the container image only needs the runnable jar.
RUN ./gradlew --no-daemon -Penv=release clean bootRepackage -x test

# ---- Runtime stage --------------------------------------------------------
FROM eclipse-temurin:8-jre

LABEL org.opencontainers.image.title="davos" \
      org.opencontainers.image.description="FTP/SFTP download automation tool" \
      org.opencontainers.image.source="https://github.com/Aerya/davos"

WORKDIR /app

# Persistent data: H2 database and logs live here (see conf/release).
VOLUME ["/config"]

# Default Spring Boot web port.
EXPOSE 8080

COPY --from=build /src/build/libs/davos-*.jar /app/davos.jar

ENTRYPOINT ["java", "-jar", "/app/davos.jar"]
