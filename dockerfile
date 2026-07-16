# Estágio 1: Compilação do projeto usando Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
# Compila o projeto pulando os testes para agilizar a inicialização do professor
RUN mvn clean package -DskipTests

# Estágio 2: Execução leve usando apenas o JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Porta padrão que sua API Spring Boot expõe
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]