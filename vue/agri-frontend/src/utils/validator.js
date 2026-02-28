/**
 * 密码验证工具函数
 */

/* eslint-disable no-useless-escape */
// 密码允许的字符：字母、数字、常用特殊字符
const PASSWORD_PATTERN = /^[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]+$/;

// 密码最小长度
const PASSWORD_MIN_LENGTH = 6;

// 密码最大长度
const PASSWORD_MAX_LENGTH = 20;

/**
 * 验证密码格式
 * @param {string} password - 待验证的密码
 * @returns {Object} - { valid: boolean, message: string }
 */
export function validatePassword(password) {
  if (!password) {
    return { valid: false, message: '密码不能为空' };
  }
  
  if (password.length < PASSWORD_MIN_LENGTH) {
    return { valid: false, message: `密码长度不能少于${PASSWORD_MIN_LENGTH}位` };
  }
  
  if (password.length > PASSWORD_MAX_LENGTH) {
    return { valid: false, message: `密码长度不能超过${PASSWORD_MAX_LENGTH}位` };
  }
  
  if (!PASSWORD_PATTERN.test(password)) {
    return { valid: false, message: '密码只能包含字母、数字和特殊字符（!@#$%^&*等）' };
  }
  
  return { valid: true, message: '' };
}

/**
 * 过滤密码输入 - 只保留合法字符
 * @param {string} value - 输入的值
 * @returns {string} - 过滤后的值
 */
export function filterPasswordInput(value) {
  if (!value) return '';
  // 过滤掉非法字符（只保留字母、数字、特殊字符）
  return value.replace(/[^a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/g, '');
}

/**
 * 密码强度检测
 * @param {string} password - 密码
 * @returns {Object} - { level: 1-4, text: string, color: string }
 */
export function checkPasswordStrength(password) {
  if (!password || password.length < PASSWORD_MIN_LENGTH) {
    return { level: 0, text: '弱', color: '#f44336' };
  }
  
  let score = 0;
  
  // 长度评分
  if (password.length >= 8) score += 1;
  if (password.length >= 12) score += 1;
  
  // 包含小写字母
  if (/[a-z]/.test(password)) score += 1;
  
  // 包含大写字母
  if (/[A-Z]/.test(password)) score += 1;
  
  // 包含数字
  if (/[0-9]/.test(password)) score += 1;
  
  // 包含特殊字符
  if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/.test(password)) score += 1;
  
  if (score <= 2) {
    return { level: 1, text: '弱', color: '#f44336' };
  } else if (score <= 4) {
    return { level: 2, text: '中', color: '#ff9800' };
  } else if (score <= 5) {
    return { level: 3, text: '强', color: '#4caf50' };
  } else {
    return { level: 4, text: '非常强', color: '#2e7d32' };
  }
}

export default {
  validatePassword,
  filterPasswordInput,
  checkPasswordStrength,
  PASSWORD_MIN_LENGTH,
  PASSWORD_MAX_LENGTH
};
