FROM eclipse-temurin:17
WORKDIR /app
COPY . .
RUN javac -cp ".:postgresql-42.7.3.jar" App.java
EXPOSE 8080
ENTRYPOINT ["java", "-cp", ".:postgresql-42.7.3.jar", "App"]
