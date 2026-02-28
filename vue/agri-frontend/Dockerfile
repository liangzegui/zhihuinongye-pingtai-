# =============================================
# Vue 前端 Docker 构建
# 多阶段构建：Node.js 编译 → Nginx 托管
# =============================================

# 阶段1：构建前端资源
FROM node:18-alpine AS builder
WORKDIR /app

# 复制依赖文件并安装
COPY package*.json ./
RUN npm install --registry=https://registry.npmmirror.com

# 复制源代码和构建配置
COPY . .

# 构建生产版本
RUN npm run build

# 阶段2：Nginx 托管
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# 清除默认页面
RUN rm -rf ./*

# 从构建阶段复制编译产物
COPY --from=builder /app/dist .

# 暴露端口
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
