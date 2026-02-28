const { defineConfig } = require('@vue/cli-service')
const fs = require('fs')
const path = require('path')

// 读取项目根目录的共享配置文件
let config = {
  backend: { port: 8080, host: 'localhost' },
  frontend: { port: 8081 }
}

const configPath = path.resolve(__dirname, '../../project.config.json')
if (fs.existsSync(configPath)) {
  try {
    config = JSON.parse(fs.readFileSync(configPath, 'utf-8'))
    console.log('[Vue Config] 已加载共享配置文件: project.config.json')
  } catch (e) {
    console.warn('[Vue Config] 配置文件解析失败，使用默认配置')
  }
} else {
  console.warn('[Vue Config] 未找到 project.config.json，使用默认配置')
}

const apiBaseUrl = `http://${config.backend.host}:${config.backend.port}`
const devPort = config.frontend.port

console.log(`[Vue Config] 后端地址: ${apiBaseUrl}`)
console.log(`[Vue Config] 前端端口: ${devPort}`)

module.exports = defineConfig({
  transpileDependencies: true,
  // 开发服务器配置
  devServer: {
    port: devPort, // 前端开发服务器端口
    proxy: {
      // 认证接口特殊处理：前端 /api/auth/* -> 后端 /auth/*
      '/api/auth': {
        target: apiBaseUrl,
        changeOrigin: true,
        pathRewrite: { '^/api/auth': '/auth' }, // 移除 /api 前缀
        cookieDomainRewrite: 'localhost',
        cookiePathRewrite: '/',
        onProxyReq: (proxyReq, req) => {
          console.log(`[Proxy Auth] ${req.method} ${req.url} -> ${apiBaseUrl}/auth`)
        }
      },
      // 其他 API 接口直接转发（后端路径也是 /api/*）
      '/api': {
        target: apiBaseUrl,
        changeOrigin: true,
        cookieDomainRewrite: 'localhost',
        cookiePathRewrite: '/',
        onProxyReq: (proxyReq, req) => {
          console.log(`[Proxy] ${req.method} ${req.url} -> ${apiBaseUrl}`)
        }
      }
    }
  }
})