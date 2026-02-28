<template>
  <!-- 原有模板代码不变 -->
  <div class="login-page">
    <div class="bg-decoration"></div>
    <div class="login-container">
      <div class="platform-logo">
        <i class="icon-leaf"></i>
        <h1>智慧农业监控平台</h1>
      </div>
      <div class="login-header">
        <h2>账户登录</h2>
        <p class="subtitle">请输入账号信息，登录后查看农场环境数据</p>
      </div>
      <form @submit.prevent="handleLogin" novalidate class="login-form">
        <div class="form-item">
          <div class="input-wrapper">
            <i class="icon icon-user"></i>
            <input 
              id="username" 
              type="text" 
              v-model.trim="username" 
              placeholder=" " 
              class="form-input"
            />
            <label for="username" class="floating-label">请输入用户名</label>
          </div>
        </div>
        <div class="form-item">
          <div class="input-wrapper">
            <i class="icon icon-lock"></i>
            <input 
              id="password" 
              :type="showPassword ? 'text' : 'password'" 
              v-model="password" 
              placeholder=" " 
              class="form-input"
            />
            <label for="password" class="floating-label">请输入密码</label>
            <button 
              type="button" 
              class="toggle-btn" 
              @click="showPassword = !showPassword"
              aria-label="显示/隐藏密码"
            >
              <el-icon class="eye-icon"><Hide v-if="showPassword" /><View v-else /></el-icon>
            </button>
          </div>
        </div>

        <div class="remember-row">
          <label class="remember-label">
            <input
              type="checkbox"
              v-model="rememberMe"
              @change="handleRememberChange"
            />
            <span>记住我</span>
          </label>
          <button
            v-if="rememberMe && username"
            type="button"
            class="clear-remember-btn"
            @click="clearSavedUsername"
          >
            清除记住
          </button>
        </div>

        <button type="submit" :disabled="loading" class="login-btn">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '登录中...' : '进入平台' }}
        </button>
        <div class="form-footer">
          <router-link to="/register" class="register-link">
            还没有账号？注册新账户
          </router-link>
        </div>
      </form>
      <div v-if="message" class="message" :class="{ error: messageType === 'error', success: messageType === 'success' }">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script>
import { loginApi } from '@/api/auth';
import { setToken, setUsername, setRole, clearAuthInfo } from '@/utils/token';
import { View, Hide } from '@element-plus/icons-vue';
import { filterPasswordInput } from '@/utils/validator';

export default {
  name: 'UserLogin',
  components: {
    View,
    Hide
  },
  data() {
    return {
      username: '',
      password: '',
      message: '',
      messageType: 'error', // 'error' | 'warning' | 'success'
      loading: false,
      showPassword: false,
      rememberMe: false,
      loginAttempts: 0,
      isLocked: false
    };
  },
  computed: {
    isFormValid() {
      return this.username.trim().length > 0 && this.password.length > 0;
    },
    usernameError() {
      if (this.username.trim().length === 0) return '';
      if (this.username.length < 2) return '用户名至少需要2个字符';
      if (this.username.length > 20) return '用户名不能超过20个字符';
      // 允许中文、字母、数字、下划线、@和点
      if (!/^[\u4e00-\u9fa5a-zA-Z0-9_@.]+$/.test(this.username)) return '用户名只能包含中文、字母、数字、下划线、@和点';
      return '';
    },
    passwordError() {
      if (this.password.length === 0) return '';
      if (this.password.length < 6) return '密码至少需要6个字符';
      if (this.password.length > 32) return '密码不能超过32个字符';
      return '';
    }
  },
  mounted() {
    // 登录页面加载时清除旧的登录Token，避免Token过期干扰登录
    clearAuthInfo();
    // 检查是否有保存的用户名
    const savedUsername = localStorage.getItem('agri_saved_username');
    const rememberFlag = localStorage.getItem('agri_remember_me') === 'true';
    if (savedUsername) {
      this.username = savedUsername;
      this.rememberMe = true;
    } else {
      this.rememberMe = rememberFlag;
    }
  },
  watch: {
    // 监听密码输入，过滤非法字符
    password(newVal) {
      const filtered = filterPasswordInput(newVal);
      if (filtered !== newVal) {
        this.password = filtered;
      }
    }
  },
  methods: {
    async handleLogin() {
      this.message = '';
      
      // 基础校验
      if (!this.username.trim() || !this.password.trim()) {
        this.message = '用户名和密码不能为空';
        this.messageType = 'error';
        return;
      }
      
      // 格式校验
      if (this.usernameError) {
        this.message = this.usernameError;
        this.messageType = 'error';
        return;
      }
      
      if (this.passwordError) {
        this.message = this.passwordError;
        this.messageType = 'error';
        return;
      }

      this.loading = true;
      try {
        // 调用登录接口
        const resData = await loginApi(this.username, this.password);
        console.log('[Login] 登录响应:', resData);

        if (resData.code === 200 && resData.data?.token) {
          // 登录成功 — token/username 在 data 字段内（Result<T> 格式）
          setToken(resData.data.token);
          setUsername(resData.data.username || this.username);
          setRole(resData.data.role || 'user');
          
          // 保存用户名（如果勾选了"记住我"）
          if (this.rememberMe) {
            localStorage.setItem('agri_saved_username', this.username);
            localStorage.setItem('agri_remember_me', 'true');
          } else {
            localStorage.removeItem('agri_saved_username');
            localStorage.removeItem('agri_remember_me');
          }
          
          this.message = '登录成功，正在跳转...';
          this.messageType = 'success';
          
          // 延迟跳转，让用户看到成功提示
          setTimeout(() => {
            const redirectPath = this.$route.query.redirect || '/home';
            this.$router.push(redirectPath);
          }, 500);
        } else {
          // 登录失败（服务端已负责计数和锁定，msg 中包含剩余次数提示）
          this.message = resData.msg || '登录失败：用户名或密码错误';
          this.messageType = 'error';
          clearAuthInfo();
        }
      } catch (err) {
        console.error('[Login] 登录异常:', err);
        
        // 处理网络错误和超时
        if (err.message === 'Network Error') {
          this.message = '网络连接失败，请检查网络设置';
        } else if (err.code === 'ECONNABORTED') {
          this.message = '请求超时，请检查网络连接';
        } else {
          this.message = err?.response?.data?.msg || err?.message || '登录失败，请重试';
        }
        this.messageType = 'error';
        clearAuthInfo();
      } finally {
        this.loading = false;
      }
    },
    
    // 清除保存的用户名
    clearSavedUsername() {
      localStorage.removeItem('agri_saved_username');
      localStorage.removeItem('agri_remember_me');
      this.rememberMe = false;
    },

    // 切换“记住我”状态
    handleRememberChange() {
      if (!this.rememberMe) {
        this.clearSavedUsername();
      } else {
        localStorage.setItem('agri_remember_me', 'true');
      }
    }
  }
};
</script>

<style scoped>
/* 原有样式代码不变 */
:root {
  --primary-color: #2e7d32; /* 深绿主色 */
  --primary-light: #4caf50; /* 浅绿 */
  --accent-color: #00acc1; /* 科技蓝辅助色 */
  --bg-color: #f1f8e9; /* 浅绿背景 */
  --text-dark: #1b5e20; /* 深绿文字 */
  --text-light: #f1f8e9; /* 浅色文字 */
}
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, var(--bg-color) 0%, #e8f5e9 100%);
  padding: 20px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}
.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%2381c784' fill-opacity='0.1'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  z-index: 0;
}
.login-container {
  width: 420px;
  background-color: #fff;
  border-radius: 12px;
  padding: 30px 40px;
  box-shadow: 0 8px 24px rgba(46, 125, 50, 0.15);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  z-index: 1;
}
.login-container:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 32px rgba(46, 125, 50, 0.2);
}
.platform-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  gap: 10px;
}
.icon-leaf {
  font-size: 28px;
  color: var(--primary-color);
  animation: pulse 3s infinite;
}
.platform-logo h1 {
  margin: 0;
  color: var(--text-dark);
  font-size: 20px;
  font-weight: 600;
}
.login-header {
  text-align: center;
  margin-bottom: 30px;
}
.login-header h2 {
  margin: 0 0 10px;
  color: var(--text-dark);
  font-size: 24px;
  font-weight: 600;
}
.subtitle {
  color: #558b2f;
  margin: 0;
  font-size: 14px;
}
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-item {
  width: 100%;
}
.input-wrapper {
  position: relative;
  width: 100%;
}
.form-input {
  width: 100%;
  padding: 14px 14px 14px 48px;
  border: 1px solid #c8e6c9;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s ease;
  box-sizing: border-box;
}
.form-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.1);
  transform: scale(1.01);
}
.icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--primary-color);
  font-size: 18px;
}
.icon-user::before {
  content: "👤";
}
.icon-lock::before {
  content: "🔒";
}
.icon-eye {
  font-size: 16px;
}
.eye-icon {
  font-size: 18px;
  font-style: normal;
  line-height: 1;
}

/* 浮动标签样式 */
.floating-label {
  position: absolute;
  left: 46px;
  top: 50%;
  transform: translateY(-50%);
  color: #9e9e9e;
  font-size: 14px;
  pointer-events: none;
  transition: all 0.2s ease;
  background: transparent;
  padding: 0 4px;
}

/* 输入框有内容或聚焦时，标签缩小上移 */
.form-input:focus + .floating-label,
.form-input:not(:placeholder-shown) + .floating-label {
  top: 0;
  font-size: 12px;
  color: var(--primary-color);
  background: white;
}

.toggle-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  cursor: pointer;
  color: #66bb6a;
  padding: 4px;
  transition: color 0.2s ease;
}
.toggle-btn:hover {
  color: var(--primary-color);
}

.remember-row {
  margin-top: -8px;
  margin-bottom: -4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.remember-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #558b2f;
  font-size: 14px;
  cursor: pointer;
  user-select: none;
}

.remember-label input[type="checkbox"] {
  width: 14px;
  height: 14px;
  accent-color: var(--primary-color);
}

.clear-remember-btn {
  border: none;
  background: transparent;
  color: #66bb6a;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
}

.clear-remember-btn:hover {
  color: var(--primary-color);
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  padding: 14px;
  background-color: var(--primary-color);
  color: var(--text-light);
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.login-btn:hover:not(:disabled) {
  background-color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(46, 125, 50, 0.3);
}
.login-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.login-btn:disabled {
  background-color: #a5d6a7;
  cursor: not-allowed;
  box-shadow: none;
}
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
}
.form-footer {
  text-align: center;
  margin-top: 10px;
}
.register-link {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s ease;
}
.register-link:hover {
  text-decoration: underline;
  color: var(--primary-light);
}
.message {
  margin-top: 15px;
  padding: 10px 15px;
  border-radius: 6px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
  animation: fadeIn 0.3s ease;
}
.error {
  background-color: #ffebee;
  color: #c62828;
  border: 1px solid #ef9a9a;
}
.success {
  background-color: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #a5d6a7;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}
@media (max-width: 450px) {
  .login-container {
    width: 100%;
    padding: 25px 20px;
  }
  .form-input {
    padding: 12px 12px 12px 42px;
    font-size: 14px;
  }
}
</style>
