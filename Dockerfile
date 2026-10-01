# Этап 1: Сборка
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY App.java .
RUN javac App.java

# Этап 2: Запуск
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/App.class .
EXPOSE 8080
CMD ["java", "App"]
