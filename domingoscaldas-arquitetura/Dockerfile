# Receita da imagem: o JAR e gerado antes, com ./mvnw package (o Docker so empacota e executa)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
