// 导入Vue核心函数、根组件及路由
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
// 引入 Element Plus 及其样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/styles/pages.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// 创建Vue实例，挂载 Pinia 状态管理、路由、Element Plus
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
