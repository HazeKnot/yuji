FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY yuji-common/pom.xml yuji-common/
COPY yuji-auth/pom.xml yuji-auth/
COPY yuji-profile/pom.xml yuji-profile/
COPY yuji-server/pom.xml yuji-server/
RUN mvn -q dependency:go-offline || true
COPY yuji-common yuji-common
COPY yuji-auth yuji-auth
COPY yuji-profile yuji-profile
COPY yuji-server yuji-server
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/yuji-server/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
