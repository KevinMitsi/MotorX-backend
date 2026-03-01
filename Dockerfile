#
# Etapa de construcción
#
FROM eclipse-temurin:21-jdk AS build
WORKDIR /home/gradle/project

# Copiar solo archivos necesarios primero para aprovechar el cache de dependencias
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Copiar el resto del código fuente
COPY src src

# Construir el archivo JAR
RUN ./gradlew --no-daemon bootJar

#
# Etapa de empaquetado
#
FROM eclipse-temurin:21-jre
ENV APP_HOME=/app
WORKDIR ${APP_HOME}

# Argumento opcional para puerto
ARG PORT=8080
ENV PORT=${PORT}

# Copiar el JAR generado desde la etapa de construcción
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]