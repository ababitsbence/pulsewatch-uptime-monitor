# ---- build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:25-jre
# apt-get upgrade pulls in OS security fixes that are newer than the base image
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app app

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
# Numeric user, so that Kubernetes can verify the container does not run as root
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]