<template>
  <nav class="navbar">
    <div class="navbar-left">
      <i class="icon-leaf"></i>
      <span class="navbar-title">智慧农业监控平台</span>
    </div>
    <div class="navbar-menu">
      <router-link to="/realtime" class="menu-item" :class="{ active: $route.name === 'RealTime' }">
        实时数据
      </router-link>
      <router-link to="/historical" class="menu-item" :class="{ active: $route.name === 'HistoricalData' }">
        历史数据
      </router-link>
      <router-link to="/analysis" class="menu-item" :class="{ active: $route.name === 'DataAnalysis' }">
        数据趋势
      </router-link>
      <router-link to="/warning" class="menu-item" :class="{ active: $route.name === 'WarningLogs' }">
        警告日志
      </router-link>
      <router-link to="/control-history" class="menu-item" :class="{ active: $route.name === 'ControlHistory' }">
        控制记录
      </router-link>
      <router-link to="/settings" class="menu-item" :class="{ active: $route.name === 'Settings' }">
        系统设置
      </router-link>
      <router-link
        v-if="isAdmin"
        to="/admin"
        class="menu-item"
        :class="{ active: $route.name === 'AdminManage' }"
      >
        管理员中心
      </router-link>
      <router-link to="/profile" class="menu-item" :class="{ active: $route.name === 'PersonalInfo' }">
        个人信息
      </router-link>
    </div>
    <div class="navbar-right">
      <span class="username">{{ username }}</span>
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </div>
  </nav>
</template>

<script>
import { clearAuthInfo } from '@/utils/token'
export default {
  name: 'AgriNavbar', // 关键修改：组件名从"Navbar"改为"AgriNavbar"（多单词命名）
  data() {
    return {
      username: localStorage.getItem('agri_platform_username') || '用户',
      role: localStorage.getItem('agri_platform_role') || 'user'
    }
  },
  computed: {
    isAdmin() {
      return this.role === 'admin'
    }
  },
  methods: {
    handleLogout() {
      // 清除登录状态（token、用户名）
      try { clearAuthInfo() } catch (e) {
        // eslint-disable-next-line no-console
        console.warn('清除本地认证信息失败', e)
      }
      // 跳转到登录页
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  background-color: #2e7d32;
  color: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.navbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.icon-leaf {
  font-size: 24px;
  animation: pulse 3s infinite;
}
.navbar-title {
  font-size: 18px;
  font-weight: 600;
}
.navbar-menu {
  display: flex;
  gap: 30px;
}
.menu-item {
  color: white;
  text-decoration: none;
  font-size: 15px;
  padding: 8px 0;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}
.menu-item.active {
  border-bottom: 2px solid #e8f5e9;
  color: #e8f5e9;
}
.menu-item:hover {
  color: #c8e6c9;
}
.navbar-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.username {
  font-size: 14px;
}
.logout-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  background-color: #4caf50;
  color: white;
  cursor: pointer;
  transition: background-color 0.3s;
}
.logout-btn:hover {
  background-color: #81c784;
}
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}
</style>
