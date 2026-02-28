import axios from 'axios';
import { clearAuthInfo } from '@/utils/token';
import router from '@/router';
import { ElMessage } from 'element-plus';

const service = axios.create({
  // 开发环境使用代理，生产环境使用环境变量配置的后端地址
  baseURL: process.env.VUE_APP_API_BASE_URL || '',
  timeout: 15000,
  withCredentials: true, // 携带Cookie/Session
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
});

// 请求拦截器：自动携带Token
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('agri_platform_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    // 添加请求时间戳用于缓存破除
    config.params = config.params || {};
    if (config.method === 'get') {
      config.params._t = Date.now();
    }
    console.debug('[API Request]', config.method?.toUpperCase(), config.url, config.params);
    return config;
  },
  (error) => {
    console.error('[API Error] 请求拦截器错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器：统一处理错误和状态
service.interceptors.response.use(
  (response) => {
    // 直接返回后端的响应体（axios的response.data即为后端返回的内容）
    const data = response.data;
    console.debug('[API Response]', response.status, data);
    return data;
  },
  (error) => {
    // 区分不同类型的错误
    if (error.response) {
      // 服务器响应了错误状态码
      const status = error.response.status;
      const errorData = error.response.data;
      const errorMsg = errorData?.message || `服务器错误 (${status})`;
      
      console.error('[API Error]', status, errorMsg);
      
      switch (status) {
        case 400:
          ElMessage.error('请求参数错误：' + errorMsg);
          break;
        case 401:
          // Token过期/未登录
          clearAuthInfo();
          ElMessage.error('登录已过期，请重新登录');
          router.push({
            path: '/login',
            query: { redirect: router.currentRoute.value.fullPath }
          });
          break;
        case 403:
          ElMessage.error('无权限访问该资源');
          break;
        case 404:
          ElMessage.error('请求的资源不存在');
          break;
        case 500:
          ElMessage.error('服务器内部错误，请稍后重试');
          break;
        default:
          ElMessage.error(errorMsg);
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      console.error('[API Error] 无响应:', error.message);
      ElMessage.error('网络连接失败，请检查网络设置');
    } else {
      // 请求构造出错
      console.error('[API Error] 请求构造失败:', error.message);
      ElMessage.error('请求发送失败：' + error.message);
    }
    return Promise.reject(error);
  }
);

export default service;