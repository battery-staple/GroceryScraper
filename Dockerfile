# Stage 1: Build the React Frontend
FROM node:20 AS frontend-build
WORKDIR /app/frontend
# Copy frontend package definitions and install dependencies
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
# Copy the rest of the frontend source and build
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the Kotlin Backend
FROM gradle:8.5-jdk21 AS backend-build
WORKDIR /app
# Copy gradle files and source code
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/ ./gradle/
COPY src/ ./src/
# Copy the built frontend into the Kotlin resources directory so Ktor can serve it
COPY --from=frontend-build /app/frontend/dist /app/src/main/resources/web
# Build the fat JAR
RUN gradle shadowJar --no-daemon

# Stage 3: Runtime Environment (Playwright Java)
FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy AS runtime
WORKDIR /app
# Copy the fat JAR from the backend build stage
COPY --from=backend-build /app/build/libs/grocery-scraper.jar ./grocery-scraper.jar

# Expose the Ktor server port
EXPOSE 8080

# Command to run the application with the web flag
CMD ["java", "-jar", "grocery-scraper.jar", "--web"]
