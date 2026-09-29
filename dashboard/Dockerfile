# Etapa 1: build de los assets estáticos con Node
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Vacío = mismo origen que nginx (ver nginx.conf, que hace proxy de /api a la app Spring Boot).
# No hace falta apuntar a un host:puerto concreto porque nginx y el navegador comparten origen.
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# Etapa 2: servir los estáticos con nginx
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
