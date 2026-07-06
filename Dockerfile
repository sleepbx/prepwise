# Stage 1: Build frontend
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install --legacy-peer-deps
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend (frontend dist is copied into static/ so Spring Boot serves it)
FROM eclipse-temurin:21-jdk-alpine AS backend
WORKDIR /app
COPY . .
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw package -DskipTests -q

# Stage 3: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/prepwise-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
