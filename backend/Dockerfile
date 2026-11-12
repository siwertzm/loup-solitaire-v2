# --- Etape 1 : build ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copie du wrapper Maven et du pom en premier pour profiter du cache Docker :
# tant que pom.xml ne change pas, les dependances ne sont pas re-telechargees.
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# --- Etape 2 : runtime ---
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# curl est necessaire au healthcheck defini dans docker-compose.yml (image jammy
# minimale, ni curl ni wget ne sont presents par defaut).
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Utilisateur non-root : bonne pratique de securite pour un conteneur expose.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# OPS-01 : sans reglage, la JVM ne donne au tas (les donnees de l'appli) que
# 25 % de la memoire du conteneur. 75 % laisse le reste a la JVM elle-meme
# (classes, threads, code compile). Plus de memoire -> arret net (Render
# redemarre le service) au lieu d'un service a moitie bloque.
# Mesure (instance Render 1 CPU / 2 Go) : voir backend/README.md, section
# Memoire et charge.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
