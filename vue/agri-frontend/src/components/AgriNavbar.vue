<template>
  <nav class="navbar">
    <div class="navbar-left">
      <router-link to="/home" class="brand-link">
        <i class="icon-leaf"></i>
        <span class="navbar-title">智慧农业监控平台</span>
      </router-link>
    </div>
    <button class="hamburger" @click="menuOpen = !menuOpen" aria-label="菜单">
      <span></span><span></span><span></span>
    </button>
    <div class="navbar-menu" :class="{ open: menuOpen }" @click="menuOpen = false">
      <router-link to="/home" class="menu-item" :class="{ active: $route.name === 'Home' }">首页</router-link>
      <router-link to="/realtime" class="menu-item" :class="{ active: $route.name === 'RealTime' }">实时数据</router-link>
      <router-link to="/historical" class="menu-item" :class="{ active: $route.name === 'HistoricalData' }">历史数据</router-link>
      <router-link to="/analysis" class="menu-item" :class="{ active: $route.name === 'DataAnalysis' }">数据趋势</router-link>
      <router-link to="/warning" class="menu-item" :class="{ active: $route.name === 'WarningLogs' }">警告日志</router-link>
      <router-link to="/control-history" class="menu-item" :class="{ active: $route.name === 'ControlHistory' }">控制记录</router-link>
      <router-link to="/settings" class="menu-item" :class="{ active: $route.name === 'Settings' }">系统设置</router-link>
      <router-link v-if="isAdmin" to="/admin" class="menu-item" :class="{ active: $route.name === 'AdminManage' }">管理中心</router-link>
    </div>
    <div class="navbar-right">
      <div class="notification-bell" @click="$router.push('/warning')" title="查看预警">
        <span class="bell-icon">&#128276;</span>
        <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </div>
      <span class="username">{{ displayUsername }}</span>
      <button class="logout-btn" @click="handleLogout">退出</button>
    </div>
  </nav>
</template>

<script>
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'

export default {
  name: 'AgriNavbar',
  data() {
    return {
      menuOpen: false
    }
  },
  computed: {
    displayUsername() {
      const userStore = useUserStore()
      return userStore.username || '用户'
    },
    isAdmin() {
      const userStore = useUserStore()
      return userStore.role === 'admin'
    },
    unreadCount() {
      const notificationStore = useNotificationStore()
      return notificationStore.unreadCount
    }
  },
  methods: {
    handleLogout() {
      const userStore = useUserStore()
      userStore.logout()
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
  position: relative;
}
.navbar-left { display: flex; align-items: center; }
.brand-link {
  display: flex; align-items: center; gap: 10px;
  text-decoration: none; color: white;
}
.icon-leaf { font-size: 24px; }
.navbar-title { font-size: 18px; font-weight: 600; white-space: nowrap; }
.navbar-menu {
  display: flex;
  gap: 20px;
}
.menu-item {
  color: white; text-decoration: none; font-size: 14px;
  padding: 8px 0; border-bottom: 2px solid transparent;
  transition: all 0.3s; white-space: nowrap;
}
.menu-item.active { border-bottom-color: #e8f5e9; color: #e8f5e9; }
.menu-item:hover { color: #c8e6c9; }
.navbar-right {
  display: flex; align-items: center; gap: 12px;
}
.notification-bell {
  position: relative; cursor: pointer; padding: 4px;
}
.bell-icon { font-size: 20px; }
.notification-badge {
  position: absolute; top: -4px; right: -6px;
  background: #f44336; color: white;
  font-size: 11px; font-weight: 600;
  min-width: 18px; height: 18px;
  border-radius: 9px; display: flex;
  align-items: center; justify-content: center;
  padding: 0 4px;
}
.username { font-size: 14px; white-space: nowrap; }
.logout-btn {
  padding: 6px 12px; border: none; border-radius: 4px;
  background-color: #4caf50; color: white; cursor: pointer;
  transition: background-color 0.3s; white-space: nowrap;
}
.logout-btn:hover { background-color: #81c784; }
.hamburger {
  display: none; flex-direction: column; gap: 5px;
  background: none; border: none; cursor: pointer; padding: 4px;
}
.hamburger span {
  width: 24px; height: 2px; background: white; display: block;
  transition: transform 0.3s;
}
@media (max-width: 768px) {
  .hamburger { display: flex; }
  .navbar-menu {
    display: none; position: absolute; top: 60px; left: 0;
    right: 0; background: #2e7d32; flex-direction: column;
    padding: 10px 20px; gap: 0; z-index: 100;
    box-shadow: 0 4px 8px rgba(0,0,0,0.2);
  }
  .navbar-menu.open { display: flex; }
  .menu-item { padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.1); }
  .menu-item.active { border-bottom-color: rgba(255,255,255,0.1); }
  .navbar-title { font-size: 16px; }
  .username { display: none; }
}
</style>
