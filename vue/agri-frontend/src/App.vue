<template>
  <div id="app">
    <!-- 登录且非免登录路由时显示导航栏 -->
    <AgriNavbar v-if="showNavbar" />
    <!-- 路由出口：所有页面通过这里渲染 -->
    <router-view />
  </div>
</template>

<script>
import AgriNavbar from './components/AgriNavbar.vue'
import { getToken } from '@/utils/token'
import websocketService from '@/utils/websocket'
import { ElNotification } from 'element-plus'

export default {
  name: 'App',
  components: {
    AgriNavbar
  },
  computed: {
    showNavbar() {
      const token = getToken()
      const noAuth = this.$route.meta?.noAuth
      return !!token && !noAuth
    }
  },
  mounted() {
    // 订阅预警WebSocket通知，弹窗提醒用户
    this.warningHandler = (data) => {
      ElNotification({
        title: '⚠️ 环境预警',
        message: data.description || '检测到环境参数异常',
        type: 'warning',
        duration: 8000,
        position: 'top-right',
        onClick: () => {
          this.$router.push('/warning')
        }
      })
    }
    websocketService.onWarnings(this.warningHandler)
  },
  beforeUnmount() {
    if (this.warningHandler) {
      websocketService.removeListener('warnings', this.warningHandler)
    }
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin-top: 0;
  min-height: 100vh;
  background-color: #f9fbe7;
}
</style>