FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY .. .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y \
    libstdc++6 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
