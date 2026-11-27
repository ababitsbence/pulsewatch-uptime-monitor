# ---- build stage ----
FROM node:26-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- runtime stage ----
# The unprivileged nginx image is published as nginx/nginx-unprivileged. The older
# nginxinc/nginx-unprivileged name was still on nginx 1.30.6, which has known vulnerabilities.
FROM nginx/nginx-unprivileged:1.30.7-alpine
# apk upgrade pulls in OS security fixes that are newer than the base image.
# The base image runs as the unprivileged user 101, so switch to root only for this step.
USER root
RUN apk upgrade --no-cache
USER 101
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080