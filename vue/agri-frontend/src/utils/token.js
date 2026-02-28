// utils/token.js
// 定义存储key（避免硬编码，便于后续修改）
const TOKEN_KEY = 'agri_platform_token';
const USERNAME_KEY = 'agri_platform_username';
const ROLE_KEY = 'agri_platform_role';

/**
 * 存储token
 * @param {string} token - 后端返回的认证token
 */
export const setToken = (token) => {
  token ? localStorage.setItem(TOKEN_KEY, token) : localStorage.removeItem(TOKEN_KEY);
};

/**
 * 获取token
 * @returns {string} - 本地存储的token（无则返回空字符串）
 */
export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY) || '';
};

/**
 * 存储用户名
 * @param {string} username - 登录用户的用户名
 */
export const setUsername = (username) => {
  username ? localStorage.setItem(USERNAME_KEY, username) : localStorage.removeItem(USERNAME_KEY);
};

/**
 * 获取用户名
 * @returns {string} - 本地存储的用户名（无则返回空字符串）
 */
export const getUsername = () => {
  return localStorage.getItem(USERNAME_KEY) || '';
};

/**
 * 存储角色
 * @param {string} role - 用户角色（admin/user）
 */
export const setRole = (role) => {
  role ? localStorage.setItem(ROLE_KEY, role) : localStorage.removeItem(ROLE_KEY);
};

/**
 * 获取角色
 * @returns {string} - 本地存储的角色
 */
export const getRole = () => {
  return localStorage.getItem(ROLE_KEY) || '';
};

/**
 * 清除所有认证信息（登录失败/过期时用）
 */
export const clearAuthInfo = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(ROLE_KEY);
};