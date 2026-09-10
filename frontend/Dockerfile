# --- Etape 1 : build ---
FROM node:22-alpine AS build
WORKDIR /app

# Copie du manifest en premier pour profiter du cache Docker :
# tant que package*.json ne change pas, npm install n'est pas rejoue.
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

COPY . .

# URL de l'API backend, injectee au build (Angular compile la config en dur
# dans le bundle, elle ne peut pas etre changee a l'execution du conteneur).
# Voir docker-compose.yml pour la valeur passee en build arg.
ARG API_URL=http://localhost:8080
RUN sed -i "s#apiUrl: '.*'#apiUrl: '${API_URL}'#" src/environments/environment.ts \
    && npm run build -- --configuration production

# --- Etape 2 : runtime ---
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/loup-solitaire-front/browser /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=10s \
    CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
