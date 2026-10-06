# syntax=docker/dockerfile:1
# Imagen de la API. Construir desde la raíz del repo:
#   docker build -f server/Dockerfile -t tendencias-pos-api .

# 1) Compilar el jar ejecutable.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY contracts contracts
COPY server server
COPY desktop desktop
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :server:bootJar --no-daemon -q --console=plain \
 && cp "$(ls server/build/libs/*.jar | grep -v -- '-plain.jar')" /src/app.jar

# 2) Separar en capas (dependencias cambian poco; el código, en cada versión).
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /layers
COPY --from=build /src/app.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 3) Imagen final: solo JRE, usuario sin privilegios.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S tpos && adduser -S -G tpos -H -s /sbin/nologin tpos
WORKDIR /app
COPY --from=layers /layers/extracted/dependencies/ ./
COPY --from=layers /layers/extracted/spring-boot-loader/ ./
COPY --from=layers /layers/extracted/snapshot-dependencies/ ./
COPY --from=layers /layers/extracted/application/ ./
USER tpos
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-Xmx512m -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "application.jar"]
