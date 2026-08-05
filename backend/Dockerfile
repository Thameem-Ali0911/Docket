# --- Build stage ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy Maven wrapper + pom first so dependency layer is cached separately from source changes
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline || true

COPY src src
RUN ./mvnw -B clean package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# Native Tesseract engine + English trained data, required by Tess4J (see architecture.md §8)
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

# Run as non-root
RUN groupadd --system docket && useradd --system --gid docket docket
COPY --from=build /app/target/*.jar app.jar
RUN chown docket:docket app.jar
USER docket

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
