FROM eclipse-temurin:17
WORKDIR /app
COPY . .
RUN javac App.java
EXPOSE 8080
ENTRYPOINT ["java", "App"]
