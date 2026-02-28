<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人信息中心</h2>
      <p>查看和管理个人账号信息</p>
    </div>
    <div class="profile-container">
      <div class="info-card">
        <h3>账号信息</h3>
        <div class="info-item">
          <label>用户名</label>
          <span>{{ username }}</span>
        </div>
        <div class="info-item">
          <label>用户角色</label>
          <span>{{ role }}</span>
        </div>
        <div class="info-item">
          <label>注册时间</label>
          <span>{{ registerTime }}</span>
        </div>
      </div>
      <div class="pwd-card">
        <h3>修改密码</h3>
        <form @submit.prevent="handleUpdatePwd" class="pwd-form">
          <!-- 原密码输入 -->
          <div class="form-item">
            <div class="input-wrapper">
              <i class="icon icon-lock"></i>
              <input 
                id="oldPwd"
                :type="showOldPwd ? 'text' : 'password'" 
                v-model="oldPwd" 
                placeholder=" " 
                class="form-input"
                required
              />
              <label for="oldPwd" class="floating-label">请输入原密码</label>
              <button 
                type="button" 
                class="toggle-btn" 
                @click="showOldPwd = !showOldPwd"
                aria-label="显示/隐藏密码"
              >
                <el-icon class="eye-icon"><Hide v-if="showOldPwd" /><View v-else /></el-icon>
              </button>
            </div>
          </div>
          <!-- 新密码输入 -->
          <div class="form-item">
            <div class="input-wrapper">
              <i class="icon icon-lock"></i>
              <input 
                id="newPwd"
                :type="showNewPwd ? 'text' : 'password'" 
                v-model="newPwd" 
                placeholder=" " 
                class="form-input"
                required
                minlength="6"
              />
              <label for="newPwd" class="floating-label">请输入新密码（至少6位）</label>
              <button 
                type="button" 
                class="toggle-btn" 
                @click="showNewPwd = !showNewPwd"
                aria-label="显示/隐藏密码"
              >
                <el-icon class="eye-icon"><Hide v-if="showNewPwd" /><View v-else /></el-icon>
              </button>
            </div>
          </div>
          <!-- 确认密码输入 -->
          <div class="form-item">
            <div class="input-wrapper">
              <i class="icon icon-lock"></i>
              <input 
                id="confirmPwd"
                :type="showConfirmPwd ? 'text' : 'password'" 
                v-model="confirmPwd" 
                placeholder=" " 
                class="form-input"
                required
              />
              <label for="confirmPwd" class="floating-label">请确认新密码</label>
              <button 
                type="button" 
                class="toggle-btn" 
                @click="showConfirmPwd = !showConfirmPwd"
                aria-label="显示/隐藏密码"
              >
                <el-icon class="eye-icon"><Hide v-if="showConfirmPwd" /><View v-else /></el-icon>
              </button>
            </div>
          </div>
          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="loading" class="spinner"></span>
            {{ loading ? '提交中...' : '提交修改' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { getUsername, clearAuthInfo } from '@/utils/token';
import { getUserProfile, updatePassword } from '@/api/user';
import { View, Hide } from '@element-plus/icons-vue';
import { validatePassword, filterPasswordInput } from '@/utils/validator';

export default {
  name: 'PersonalInfo',
  components: {
    View,
    Hide
  },
  data() {
    return {
      username: getUsername() || '',
      role: '普通用户',
      registerTime: '--',
      oldPwd: '',
      newPwd: '',
      confirmPwd: '',
      loading: false,
      showOldPwd: false,
      showNewPwd: false,
      showConfirmPwd: false
    }
  },
  watch: {
    // 监听密码输入，过滤非法字符
    oldPwd(newVal) {
      const filtered = filterPasswordInput(newVal);
      if (filtered !== newVal) this.oldPwd = filtered;
    },
    newPwd(newVal) {
      const filtered = filterPasswordInput(newVal);
      if (filtered !== newVal) this.newPwd = filtered;
    },
    confirmPwd(newVal) {
      const filtered = filterPasswordInput(newVal);
      if (filtered !== newVal) this.confirmPwd = filtered;
    }
  },
  created() {
    this.fetchUserProfile();
  },
  methods: {
    // 获取用户信息
    async fetchUserProfile() {
      try {
        const res = await getUserProfile();
        if (res.code === 200 && res.data) {
          this.username = res.data.username || this.username;
          this.role = res.data.role === 'admin' ? '管理员' : '普通用户';
          if (res.data.createTime) {
            // 格式化时间
            this.registerTime = res.data.createTime.replace('T', ' ').substring(0, 19);
          }
        }
      } catch (err) {
        console.error('获取用户信息失败:', err);
      }
    },
    // 修改密码
    async handleUpdatePwd() {
      if (this.newPwd !== this.confirmPwd) {
        this.$message.error('两次输入的新密码不一致');
        return;
      }
      // 新密码格式校验
      const pwdValidation = validatePassword(this.newPwd);
      if (!pwdValidation.valid) {
        this.$message.error(pwdValidation.message);
        return;
      }
      if (this.oldPwd === this.newPwd) {
        this.$message.warning('新密码不能与原密码相同');
        return;
      }
      
      this.loading = true;
      try {
        const res = await updatePassword(this.oldPwd, this.newPwd);
        if (res.code === 200) {
          this.$message.success('密码修改成功，请重新登录');
          // 清空输入
          this.oldPwd = '';
          this.newPwd = '';
          this.confirmPwd = '';
          // 清除登录信息并跳转登录页
          setTimeout(() => {
            clearAuthInfo();
            this.$router.push('/login');
          }, 1500);
        } else {
          this.$message.error(res.msg || '修改失败');
        }
      } catch (err) {
        this.$message.error('修改失败：' + (err.message || '网络错误'));
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>

<style scoped>
/* CSS变量 */
:root {
  --primary-color: #2e7d32;
  --primary-light: #4caf50;
  --text-dark: #1b5e20;
}

.profile-page {
  padding: 20px;
}
.page-header {
  margin-bottom: 25px;
}
.page-header h2 {
  color: #1b5e20;
  margin: 0 0 8px;
  font-size: 22px;
}
.page-header p {
  color: #558b2f;
  margin: 0;
  font-size: 14px;
}
.profile-container {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 20px;
}
.info-card, .pwd-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(46, 125, 50, 0.1);
}
.info-card h3, .pwd-card h3 {
  color: #1b5e20;
  margin: 0 0 20px;
  font-size: 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8f5e9;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 0;
  border-bottom: 1px solid #f1f8e9;
}
.info-item label {
  font-size: 14px;
  color: #558b2f;
  width: 80px;
}
.info-item span {
  font-size: 15px;
  color: #2e7d32;
}
.pwd-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 表单项 */
.form-item {
  width: 100%;
}

/* 输入框包装器 */
.input-wrapper {
  position: relative;
  width: 100%;
}

/* 输入框样式 - 与登录页一致 */
.form-input {
  width: 100%;
  padding: 14px 48px 14px 48px;
  border: 1px solid #c8e6c9;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  color: #2e7d32;
}
.form-input:focus {
  outline: none;
  border-color: #2e7d32;
  box-shadow: 0 0 0 3px rgba(46, 125, 50, 0.1);
  transform: scale(1.01);
}

/* 图标样式 */
.icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #2e7d32;
  font-size: 18px;
}
.icon-lock::before {
  content: "🔒";
}

/* 浮动标签样式 - 与登录页一致 */
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
  color: #2e7d32;
  background: white;
}

/* 密码显示/隐藏按钮 */
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
  color: #2e7d32;
}
.eye-icon {
  font-size: 18px;
  font-style: normal;
  line-height: 1;
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 8px;
  background-color: #2e7d32;
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.submit-btn:hover:not(:disabled) {
  background-color: #4caf50;
  box-shadow: 0 4px 12px rgba(46, 125, 50, 0.3);
}
.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}
.submit-btn:disabled {
  background-color: #a5d6a7;
  cursor: not-allowed;
  box-shadow: none;
}

/* 加载动画 */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .profile-container {
    grid-template-columns: 1fr;
  }
}
</style>
