FROM eclipse-temurin:17
WORKDIR /app
COPY tags-service/App.java .
COPY postgresql-42.7.3.jar .
RUN javac -cp ".:postgresql-42.7.3.jar" App.java
CMD ["java", "-cp", ".:postgresql-42.7.3.jar", "App"]
