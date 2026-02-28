// api/auth.js
import request from '@/utils/request';

/**
 * 登录接口
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @returns {Promise<axios.Response>} 完整axios响应（含data、headers）
 */
export const loginApi = (username, password) => {
  return request.post('/api/auth/login', { username, password });
};

/**
 * 注册接口
 * @param {Object} userInfo - 注册信息（如username、password、email）
 * @returns {Promise<axios.Response>} 完整axios响应
 */
export const registerApi = (userInfo) => {
  return request.post('/api/auth/register', userInfo);
};

/**
 * 获取验证码图片URL（防缓存）
 * @returns {string} 验证码图片地址（直接用于<img :src="getCaptchaApi()" />）
 */
export const getCaptchaApi = () => {
  // 使用相对路径，通过 devServer.proxy 代理到后端
  return '/api/auth/captcha?' + new Date().getTime();
};