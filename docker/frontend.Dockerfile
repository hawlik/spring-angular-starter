# Build stage
FROM node:22-alpine AS build
WORKDIR /app

ARG PROJECT=user-ui

COPY apps/frontend/package*.json ./
RUN npm ci

COPY apps/frontend/ .
RUN npm run build:${PROJECT}

# Runtime stage
FROM nginx:alpine
ARG PROJECT=user-ui

COPY --from=build /app/dist/${PROJECT}/browser /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
