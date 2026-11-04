# ===== 构建阶段：Node 构建 dist =====
FROM node:20-slim AS builder
WORKDIR /app

# 先只拷贝依赖清单，利用层缓存安装依赖
COPY package.json package-lock.json ./
RUN npm ci

# 再拷贝源码并构建
COPY . .
RUN npm run build

# ===== 运行阶段：Nginx 托管 dist =====
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
