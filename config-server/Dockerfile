# Imagem do config-server: o JAR e gerado antes com ./mvnw package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
