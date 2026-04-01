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
import { useNotificationStore } from '@/stores/notification'

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
  watch: {
    // 监听路由变化，登录成功跳转后自动连接WebSocket
    '$route'() {
      this.ensureWebSocket()
    }
  },
  mounted() {
    // 订阅预警WebSocket通知，弹窗提醒用户
    this.warningHandler = (data) => {
      // 同步写入全局通知store
      const notificationStore = useNotificationStore()
      notificationStore.addWarning(data)

      ElNotification({
        title: '🚨 环境预警',
        message: data.description || '检测到环境参数异常',
        duration: 8000,
        position: 'top-right',
        customClass: 'warning-notify-center',
        onClick: () => {
          this.$router.push('/warning')
        }
      })
    }
    websocketService.onWarnings(this.warningHandler)

    // 已登录则立即连接WebSocket，确保全局都能收到预警推送
    this.ensureWebSocket()
  },
  beforeUnmount() {
    if (this.warningHandler) {
      websocketService.removeListener('warnings', this.warningHandler)
    }
  },
  methods: {
    /** 确保已登录时WebSocket已连接 */
    ensureWebSocket() {
      const token = getToken()
      if (token && !websocketService.isConnected()) {
        websocketService.connect().catch(() => {
          console.warn('[App] WebSocket自动连接失败，将在进入实时监控页时重试')
        })
      }
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

/* 预警弹窗红色主题 */
.warning-notify-center {
  background-color: #fef0f0 !important;
  border: 2px solid #f56c6c !important;
  box-shadow: 0 4px 20px rgba(245, 108, 108, 0.4) !important;
  min-width: 340px;
}
.warning-notify-center .el-notification__title {
  color: #e6232a !important;
  font-size: 16px !important;
  font-weight: bold !important;
}
.warning-notify-center .el-notification__content {
  color: #c0392b !important;
  font-size: 14px !important;
}

</style>