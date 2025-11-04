FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar arquivos do projeto
COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn ./.mvn

# Compilar projeto sem testes
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21
WORKDIR /app

# Copiar JAR gerado
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]