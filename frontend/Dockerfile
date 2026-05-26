# syntax=docker/dockerfile:1.6

# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app

# VITE_* env vars are baked in at build time, not at runtime.
# Set this in Railway as a Build Variable on the frontend service.
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY package.json package-lock.json ./
RUN npm ci

COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src
COPY vitest.setup.ts ./vitest.setup.ts
RUN npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Railway sets PORT; the template substitutes it at container start.
ENV PORT=8080
EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
