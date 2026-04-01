<template>
  <div class="register-page">
    <!-- 背景装饰元素 -->
    <div class="bg-decoration"></div>
    
    <div class="register-container">
      <!-- 平台标识 -->
      <div class="platform-logo">
        <i class="icon-leaf"></i>
        <h1>智慧农业监控平台</h1>
      </div>
      
      <div class="register-header">
        <h2>创建账户</h2>
        <p class="subtitle">注册后即可管理农场环境数据与监控设备</p>
      </div>
      
      <!-- 注册表单 -->
      <form @submit.prevent="handleRegister" novalidate class="register-form">
        <!-- 用户名输入 -->
        <div class="form-item">
          <div class="input-wrapper">
            <i class="icon icon-user"></i>
            <input 
              id="reg-username" 
              type="text" 
              v-model.trim="username" 
              placeholder=" " 
              class="form-input"
            />
            <label for="reg-username" class="floating-label">请设置用户名</label>
          </div>
        </div>
        
        <!-- 密码输入（支持显示/隐藏） -->
        <div class="form-item">
          <div class="input-wrapper">
            <i class="icon icon-lock"></i>
            <input 
              id="reg-password" 
              :type="showPassword ? 'text' : 'password'" 
              v-model="password" 
              placeholder=" " 
              class="form-input"
            />
            <label for="reg-password" class="floating-label">请设置密码（至少6位）</label>
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
        
        <!-- 确认密码输入 -->
        <div class="form-item">
          <div class="input-wrapper">
            <i class="icon icon-lock"></i>
            <input 
              id="reg-confirm-password" 
              :type="showPassword ? 'text' : 'password'" 
              v-model="confirmPassword" 
              placeholder=" " 
              class="form-input"
            />
            <label for="reg-confirm-password" class="floating-label">请再次输入密码</label>
          </div>
        </div>
        
        <!-- 验证码区域 -->
        <div class="form-item">
          <div class="input-wrapper captcha-wrapper">
            <i class="icon icon-captcha"></i>
            <input 
              id="captcha" 
              type="text" 
              v-model="captcha" 
              placeholder=" " 
              class="form-input"
            />
            <label for="captcha" class="floating-label">请输入验证码</label>
            <!-- 验证码图片（点击刷新） -->
            <img 
              :src="captchaUrl" 
              alt="验证码" 
              class="captcha-img"
              @click="refreshCaptcha"
              title="点击刷新验证码"
            />
          </div>
        </div>
        
        <!-- 注册按钮 -->
        <button type="submit" :disabled="loading" class="btn-primary">
          <span v-if="loading" class="btn-spinner"></span>
          {{ loading ? '注册中...' : '完成注册' }}
        </button>
        
        <!-- 跳转登录页链接 -->
        <div class="form-footer">
          <router-link to="/login" class="login-link">
            已有账号？直接登录
          </router-link>
        </div>
      </form>
      
      <!-- 错误/成功提示 -->
      <div 
        v-if="message" 
        class="message" 
        :class="{ error: isError, success: !isError }"
      >
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script>
// 导入axios请求工具
import request from '@/utils/request';
import { View, Hide } from '@element-plus/icons-vue';
import { filterPasswordInput } from '@/utils/validator';

export default {
  name: 'UserRegister',
  components: {
    View,
    Hide
  },
  data() {
    return {
      username: '', // 用户名
      password: '', // 密码
      confirmPassword: '', // 密码确认
      role: 'user', // 角色固定为普通用户（管理员账号由系统预设）
      captcha: '', // 验证码
      captchaKey: '', // 验证码Key（用于关联验证码）
      // 验证码图片URL
      captchaUrl: '',
      message: '', // 提示信息
      loading: false, // 注册按钮加载状态
      showPassword: false, // 密码显示/隐藏状态
      isError: true, // 提示类型（true：错误，false：成功）
      registerAttempts: 0 // 注册失败次数
    };
  },
  computed: {
    // 检查表单是否有效
    isFormValid() {
      return (
        this.username.trim().length >= 3 &&
        this.password.length >= 6 &&
        this.confirmPassword === this.password &&
        this.captcha.length > 0
      );
    },
    // 用户名验证错误信息
    usernameError() {
      if (this.username.trim().length === 0) return '';
      if (this.username.length < 2) return '用户名至少需要2个字符';
      if (this.username.length > 20) return '用户名不能超过20个字符';
      // 允许中文、字母、数字、下划线、@和点
      if (!/^[\u4e00-\u9fa5a-zA-Z0-9_@.]+$/.test(this.username)) return '用户名只能包含中文、字母、数字、下划线、@和点';
      return '';
    },
    // 密码强度检查
    passwordStrength() {
      if (this.password.length === 0) return 0;
      if (this.password.length < 6) return 1; // 弱
      
      let strength = 2; // 中
      if (/[a-z]/.test(this.password) && /[A-Z]/.test(this.password)) strength++;
      if (/\d/.test(this.password)) strength++;
      if (/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(this.password)) strength++;
      
      return Math.min(strength, 5);
    },
    // 密码强度标签
    passwordStrengthLabel() {
      const labels = ['', '弱', '中等', '良好', '很强', '非常强'];
      return labels[this.passwordStrength];
    },
    // 密码强度颜色
    passwordStrengthColor() {
      const colors = ['', '#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#85ce61'];
      return colors[this.passwordStrength];
    },
    // 密码错误提示
    passwordError() {
      if (this.password.length === 0) return '';
      if (this.password.length < 6) return '密码至少需要6个字符';
      if (this.password.length > 32) return '密码不能超过32个字符';
      return '';
    },
    // 确认密码错误提示
    confirmPasswordError() {
      if (this.confirmPassword.length === 0) return '';
      if (this.confirmPassword !== this.password) return '两次输入的密码不一致';
      return '';
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
  mounted() {
    // 注册页面加载时清除旧的登录Token，避免Token过期导致注册失败
    localStorage.removeItem('agri_platform_token');
    localStorage.removeItem('agri_platform_user');
    // 页面加载时生成验证码
    this.refreshCaptcha();
  },
  methods: {
    // 刷新验证码
    refreshCaptcha() {
      // 生成新的captchaKey
      this.captchaKey = this.generateUUID();
      this.captchaUrl = '/api/auth/captcha?key=' + this.captchaKey + '&t=' + new Date().getTime();
      this.captcha = ''; // 清空输入的验证码
    },
    
    // 生成UUID
    generateUUID() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    },
    
    // 解析后端错误信息（兼容不同返回格式）
    parseErrorPayload(payload) {
      if (!payload) return null;
      if (typeof payload === 'string') return payload;
      if (payload.message) return payload.message;
      if (payload.msg) return payload.msg;
      if (payload.error) return payload.error;
      // 处理errors数组/对象
      if (payload.errors) {
        if (Array.isArray(payload.errors)) {
          return payload.errors.map(e => (e.msg || e.message || e)).join('，');
        }
        if (typeof payload.errors === 'object') {
          return Object.values(payload.errors)
            .flat()
            .map(e => (typeof e === 'string' ? e : (e.msg || e.message || JSON.stringify(e))))
            .join('，');
        }
      }
      // 处理嵌套在data/info中的错误
      if (payload.data && typeof payload.data === 'object') {
        return this.parseErrorPayload(payload.data);
      }
      if (payload.info) return payload.info;
      return null;
    },
    
    // 注册逻辑
    async handleRegister() {
      this.message = '';
      this.isError = true;
      
      // 基础字段验证
      if (!this.username.trim()) {
        this.message = '请输入用户名';
        return;
      }
      if (!this.password) {
        this.message = '请设置密码';
        return;
      }
      if (!this.confirmPassword) {
        this.message = '请确认密码';
        return;
      }
      if (!this.captcha) {
        this.message = '请输入验证码';
        return;
      }
      
      // 格式验证
      if (this.usernameError) {
        this.message = this.usernameError;
        return;
      }
      
      if (this.passwordError) {
        this.message = this.passwordError;
        return;
      }
      
      if (this.confirmPasswordError) {
        this.message = this.confirmPasswordError;
        return;
      }
      
      // 密码强度建议
      if (this.passwordStrength < 2) {
        this.message = '密码强度过弱，建议包含大小写字母、数字和特殊字符';
        return;
      }
      
      this.loading = true;
      try {
        // 调用注册接口
        const res = await request.post('/api/auth/register', {
          username: this.username.trim(),
          password: this.password,
          role: this.role,
          captcha: this.captcha.trim(),
          captchaKey: this.captchaKey
        });
        
        console.log('[Register] 注册响应:', res);
        
        // 解析后端返回结果（兼容不同格式）
        const data = res?.data ?? res;
        const success = data?.code === 200 || data?.success === true || res?.status === 200;
        const serverMsg = data?.message || data?.msg || data?.info || null;
        
        if (!success) { // 注册失败
          this.registerAttempts++;
          this.isError = true;
          this.message = serverMsg || '注册失败，请检查输入信息';
          
          // 3次失败后需要等待
          if (this.registerAttempts >= 3) {
            this.message += '（尝试过多，请稍后再试）';
            setTimeout(() => {
              this.registerAttempts = 0;
            }, 60000); // 1分钟后重置
          }
          
          this.refreshCaptcha(); // 刷新验证码
          return;
        }
        
        // 注册成功
        this.registerAttempts = 0;
        this.isError = false;
        this.message = serverMsg || '注册成功，即将跳转到登录页';
        
        // 2秒后自动跳转到登录页
        setTimeout(() => {
          this.$router.push('/login');
        }, 2000);
      } catch (err) { // 捕获接口错误
        this.registerAttempts++;
        this.isError = true;
        console.error('[Register] 注册异常:', err);
        
        const respData = err?.response?.data;
        let parsedErr = this.parseErrorPayload(respData);
        
        if (!parsedErr) {
          if (err.message === 'Network Error') {
            parsedErr = '网络连接失败，请检查网络设置';
          } else if (err.code === 'ECONNABORTED') {
            parsedErr = '请求超时，请检查网络连接';
          } else {
            parsedErr = err?.message || '注册失败，请检查网络后重试';
          }
        }
        
        this.message = parsedErr;
        this.refreshCaptcha(); // 刷新验证码
      } finally { // 关闭加载状态
        this.loading = false;
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

/* 页面背景 */
.register-page {
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

.register-page::before {
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

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%233a7d44' fill-opacity='0.06'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  z-index: 0;
}

/* 注册容器 */
.register-container {
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

.register-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e);
  border-radius: 20px 20px 0 0;
}

.register-container:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 50px rgba(26, 71, 42, 0.18);
}

/* 平台标识 */
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

/* 注册标题区 */
.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-header h2 {
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

/* 表单样式 */
.register-form {
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

/* 输入框样式 */
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

/* 下拉选择框样式 */
.select-wrapper {
  display: flex;
  align-items: center;
}

.form-select {
  width: 100%;
  padding: 16px 16px 16px 50px;
  border: 1px solid rgba(71, 85, 99, 0.2);
  border-radius: 12px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.8);
  appearance: none;
  transition: all 0.3s ease;
  box-sizing: border-box;
  cursor: pointer;
}

/* 下拉框箭头 */
.select-wrapper::after {
  content: "▼";
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  font-size: 12px;
  pointer-events: none;
}

.form-select:focus {
  outline: none;
  border-color: #3a7d44;
  box-shadow: 0 0 0 4px rgba(58, 125, 68, 0.1);
}

/* 图标样式 */
.icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #3a7d44;
  font-size: 18px;
}

/* 图标内容 */
.icon-user::before {
  content: "👤";
}
.icon-lock::before {
  content: "🔒";
}
.icon-role::before {
  content: "👨‍💼";
}
.icon-captcha::before {
  content: "🌾";
}
.eye-icon {
  font-size: 18px;
  font-style: normal;
}

/* 浮动标签样式 */
.floating-label {
  position: absolute;
  left: 48px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
  font-size: 14px;
  pointer-events: none;
  transition: all 0.2s ease;
  background: transparent;
  padding: 0 6px;
}

/* 输入框有内容或聚焦时，标签缩小上移 */
.form-input:focus + .floating-label,
.form-input:not(:placeholder-shown) + .floating-label {
  top: 0;
  font-size: 12px;
  color: #3a7d44;
  background: white;
}

/* 密码切换按钮 */
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

/* 验证码区域样式 */
.captcha-wrapper {
  display: flex;
  align-items: center;
}

.captcha-img {
  width: 120px;
  height: 42px;
  margin-left: 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: transform 0.2s ease;
  object-fit: contain;
  border: 1px solid rgba(71, 85, 99, 0.1);
}

.captcha-img:hover {
  transform: scale(1.05);
}

/* 表单底部 */
.form-footer {
  text-align: center;
  margin-top: 12px;
}

.login-link {
  color: #3a7d44;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.login-link:hover {
  text-decoration: underline;
  color: #1a472a;
}

/* 提示信息 */
.message {
  margin-top: 18px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
  animation: fadeIn 0.3s ease;
}

/* 错误提示 */
.error {
  background: linear-gradient(135deg, rgba(254, 226, 226, 0.9), rgba(254, 202, 202, 0.9));
  color: #b91c1c;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

/* 成功提示 */
.success {
  background: linear-gradient(135deg, rgba(220, 252, 231, 0.9), rgba(187, 247, 208, 0.9));
  color: #166534;
  border: 1px solid rgba(34, 197, 94, 0.2);
}

/* 动画定义 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

/* 响应式调整 */
@media (max-width: 450px) {
  .register-container {
    width: 100%;
    padding: 28px 24px;
    border-radius: 16px;
  }

  .form-input, .form-select {
    padding: 14px 14px 14px 44px;
    font-size: 14px;
  }
}
</style>