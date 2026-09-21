# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm config set registry https://registry.npmmirror.com && npm ci || npm install
COPY . .
RUN npm run build

# ---------- 运行阶段(非 root) ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build /build/dist /usr/share/nginx/html
# 非 root 运行:临时目录与 pid 放到可写位置
RUN mkdir -p /tmp/nginx-client-body /tmp/nginx-proxy /tmp/nginx-fastcgi /var/cache/nginx \
    && chown -R 101:101 /usr/share/nginx/html /var/cache/nginx /tmp/nginx-*
USER 101
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1
