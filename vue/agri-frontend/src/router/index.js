import { createRouter, createWebHistory } from 'vue-router';
import { getToken, getRole } from '@/utils/token';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { noAuth: true, title: '登录' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '农场监控仪表盘' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { noAuth: true, title: '注册' }
  },
  {
    path: '/',
    redirect: () => {
      // 已登录用户跳转主控台，未登录用户跳转登录页
      const token = getToken()
      return token ? '/home' : '/login'
    }
  },
  {
    path: '/realtime',
    name: 'RealTime',
    component: () => import('../views/RealTime.vue'),
    meta: { title: '实时环境数据' }
  },
  {
    path: '/historical',
    name: 'HistoricalData',
    component: () => import('../views/HistoricalData.vue'),
    meta: { title: '历史数据查询' }
  },
  {
    path: '/analysis',
    name: 'DataAnalysis',
    component: () => import('../views/DataAnalysis.vue'),
    meta: { title: '数据趋势分析' }
  },
  {
    path: '/warning',
    name: 'WarningLogs',
    component: () => import('../views/WarningLogs.vue'),
    meta: { title: '警告日志记录' }
  },
  {
    path: '/profile',
    name: 'PersonalInfo',
    component: () => import('../views/PersonalInfo.vue'),
    meta: { title: '个人信息中心' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: { title: '系统设置' }
  },
  {
    path: '/control-history',
    name: 'ControlHistory',
    component: () => import('../views/ControlHistory.vue'),
    meta: { title: '设备控制记录' }
  },
  {
    path: '/admin',
    name: 'AdminManage',
    component: () => import('../views/AdminManage.vue'),
    meta: { title: '管理员中心', requiresAdmin: true }
  },
  // 404 兜底路由（必须放最后）
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
    meta: { noAuth: true, title: '页面未找到' }
  }
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
});

// 路由守卫：未登录用户拦截
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - 智慧农业监控平台';
  }

  // 无需登录的页面（如登录、注册）直接放行
  if (to.meta.noAuth) {
    return next();
  }

  // 需要登录：验证Token是否存在
  const token = getToken();
  if (token) {
    if (to.meta.requiresAdmin) {
      const role = getRole();
      if (role !== 'admin') {
        return next({ path: '/home' });
      }
    }
    next(); // 有Token，放行
  } else {
    // 无Token，跳转到登录页，并记录当前地址（登录后返回）
    next({ 
      path: '/login', 
      query: { redirect: to.fullPath } 
    });
  }
});

export default router;
