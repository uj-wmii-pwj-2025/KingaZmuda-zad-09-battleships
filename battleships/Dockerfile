FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/target/battleships-1.0.0.jar .

CMD ["java", "-cp", "battleships-1.0.0.jar", "kingazm.app.Main", "-mode", "server", "-port", "12345"]
