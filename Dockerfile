# === Build ===
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper + POM files first (layer cache)
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
COPY shared/pom.xml shared/
COPY identity-api/pom.xml identity-api/
COPY culinary-api/pom.xml culinary-api/
COPY social-api/pom.xml social-api/
COPY identity/pom.xml identity/
COPY culinary/pom.xml culinary/
COPY social/pom.xml social/
COPY notification/pom.xml notification/
COPY application/pom.xml application/

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# Copy source and build
COPY . .
RUN ./mvnw package -DskipTests -B -q -pl application -am

# === Runtime ===
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S chefkix && adduser -S chefkix -G chefkix
COPY --from=build /app/application/target/*.jar app.jar
RUN chown chefkix:chefkix app.jar

USER chefkix
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
