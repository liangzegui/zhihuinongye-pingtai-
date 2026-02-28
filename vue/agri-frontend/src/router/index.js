import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import RealTime from '../views/RealTime.vue';
// 引入其他页面（如HistoricalData、DataAnalysis等）
import HistoricalData from '../views/HistoricalData.vue';
import DataAnalysis from '../views/DataAnalysis.vue';
import WarningLogs from '../views/WarningLogs.vue';
import PersonalInfo from '../views/PersonalInfo.vue';
import Register from '../views/Register.vue';
import Home from '../views/Home.vue'; // 引入主控制台组件
import Settings from '../views/Settings.vue'; // 系统设置页面
import NotFound from '../views/NotFound.vue'; // 404页面
import AdminManage from '../views/AdminManage.vue'; // 管理员中心
import ControlHistory from '../views/ControlHistory.vue'; // 设备控制记录
import { getToken, getRole } from '@/utils/token';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { noAuth: true, title: '登录' } // 标记为"无需登录"
  },
  // 新增：主控制台（农场监控仪表盘）路由
  {
    path: '/home', // 主控制台路径
    name: 'Home',
    component: Home,
    meta: { title: '农场监控仪表盘' } // 需登录（未加noAuth即默认需要）
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { noAuth: true, title: '注册' } // 标记为"无需登录"
  },
  {
    path: '/',
    redirect: '/login' // 根路径默认跳登录页
  },
  {
    path: '/realtime',
    name: 'RealTime',
    component: RealTime,
    meta: { title: '实时环境数据' } // 需登录
  },
  {
    path: '/historical',
    name: 'HistoricalData',
    component: HistoricalData,
    meta: { title: '历史数据查询' } // 需登录
  },
  {
    path: '/analysis',
    name: 'DataAnalysis',
    component: DataAnalysis,
    meta: { title: '数据趋势分析' } // 需登录
  },
  {
    path: '/warning',
    name: 'WarningLogs',
    component: WarningLogs,
    meta: { title: '警告日志记录' } // 需登录
  },
  {
    path: '/profile',
    name: 'PersonalInfo',
    component: PersonalInfo,
    meta: { title: '个人信息中心' } // 需登录
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { title: '系统设置' } // 需登录
  },
  {
    path: '/control-history',
    name: 'ControlHistory',
    component: ControlHistory,
    meta: { title: '设备控制记录' }
  },
  {
    path: '/admin',
    name: 'AdminManage',
    component: AdminManage,
    meta: { title: '管理员中心', requiresAdmin: true }
  },
  // 404 兜底路由（必须放最后）
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
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
