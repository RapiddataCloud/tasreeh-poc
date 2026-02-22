# ---------- Build stage ----------
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ---------- Nginx stage ----------
FROM nginx:1.27-alpine

# Remove default nginx config + default html
RUN rm -f /etc/nginx/conf.d/default.conf \
 && rm -rf /usr/share/nginx/html/*

# SPA nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Angular outputMode=server => serve the "browser" folder
COPY --from=build /app/dist/tasreeh-po-approval-ui/browser/ /usr/share/nginx/html/

# Angular may generate CSR entry file; make it the default index.html for SPA hosting
RUN if [ -f /usr/share/nginx/html/index.csr.html ]; then \
      rm -f /usr/share/nginx/html/index.html && \
      mv /usr/share/nginx/html/index.csr.html /usr/share/nginx/html/index.html ; \
    fi

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
