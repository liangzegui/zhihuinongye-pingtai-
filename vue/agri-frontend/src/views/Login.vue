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
              placeholder="请输入用户名" 
              class="form-input"
            />
          </div>
        </div>
        <div class="form-item">
          <div class="input-wrapper">
            <i class="icon icon-lock"></i>
            <input 
              id="password" 
              :type="showPassword ? 'text' : 'password'" 
              v-model="password" 
              placeholder="请输入密码" 
              class="form-input"
            />
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

        <button type="submit" :disabled="loading" class="btn-primary">
          <span v-if="loading" class="btn-spinner"></span>
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
import { clearAuthInfo } from '@/utils/token';
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
    // 只有通过路由守卫跳转过来时才清除Token（携带redirect参数），避免已登录用户手动访问/login时被Token被清除
    if (this.$route.query.redirect) {
      clearAuthInfo();
    }
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
        if (resData.code === 200 && resData.data?.token) {
          // 登录成功 — token/username 在 data 字段内（Result<T> 格式）
          const { useUserStore } = await import('@/stores/user')
          const userStore = useUserStore()
          userStore.setAuth(resData.data.token, resData.data.username || this.username, resData.data.role || 'user')
          
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
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿)
   Text: #1e293b / #475569 / #64748b */

.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  padding: 20px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(ellipse at 30% 20%, rgba(58, 125, 68, 0.08) 0%, transparent 50%),
              radial-gradient(ellipse at 70% 80%, rgba(15, 118, 110, 0.06) 0%, transparent 50%);
  pointer-events: none;
}

.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%233a7d44' fill-opacity='0.06'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  z-index: 0;
}

.login-container {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 36px 44px;
  box-shadow: 0 12px 40px rgba(26, 71, 42, 0.12);
  border: 1px solid rgba(71, 85, 99, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  z-index: 1;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e);
  border-radius: 20px 20px 0 0;
}

.login-container:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 50px rgba(26, 71, 42, 0.18);
}

.platform-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  gap: 12px;
}

.icon-leaf {
  font-size: 32px;
  color: #3a7d44;
  animation: pulse 3s infinite;
  filter: drop-shadow(0 2px 4px rgba(58, 125, 68, 0.3));
}

.platform-logo h1 {
  margin: 0;
  color: #1a472a;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h2 {
  margin: 0 0 10px;
  color: #1a472a;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.subtitle {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
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
  padding: 16px 16px 16px 50px;
  border: 1px solid rgba(71, 85, 99, 0.2);
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.8);
}

.form-input:focus {
  outline: none;
  border-color: #3a7d44;
  box-shadow: 0 0 0 4px rgba(58, 125, 68, 0.1);
  background: #fff;
}

.icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #3a7d44;
  font-size: 18px;
}

.icon-user::before {
  content: "👤";
}

.icon-lock::before {
  content: "🔒";
}

.eye-icon {
  font-size: 18px;
  font-style: normal;
  line-height: 1;
}

.toggle-btn {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  cursor: pointer;
  color: #64748b;
  padding: 4px;
  transition: color 0.2s ease;
}

.toggle-btn:hover {
  color: #3a7d44;
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
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  user-select: none;
}

.remember-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #3a7d44;
}

.clear-remember-btn {
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 4px;
  transition: color 0.2s;
}

.clear-remember-btn:hover {
  color: #3a7d44;
  text-decoration: underline;
}

.form-footer {
  text-align: center;
  margin-top: 12px;
}

.register-link {
  color: #3a7d44;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.register-link:hover {
  text-decoration: underline;
  color: #1a472a;
}

.message {
  margin-top: 18px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
  animation: fadeIn 0.3s ease;
}

.error {
  background: linear-gradient(135deg, rgba(254, 226, 226, 0.9), rgba(254, 202, 202, 0.9));
  color: #b91c1c;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.success {
  background: linear-gradient(135deg, rgba(220, 252, 231, 0.9), rgba(187, 247, 208, 0.9));
  color: #166534;
  border: 1px solid rgba(34, 197, 94, 0.2);
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
    padding: 28px 24px;
    border-radius: 16px;
  }
  .form-input {
    padding: 14px 14px 14px 44px;
    font-size: 14px;
  }
}
</style>
