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
          <button type="submit" class="btn-primary" :disabled="loading">
            <span v-if="loading" class="btn-spinner"></span>
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
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿) */

.profile-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.profile-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 50%;
  background: radial-gradient(ellipse at top left, rgba(58, 125, 68, 0.06) 0%, transparent 70%);
  pointer-events: none;
}

/* ========== Page Header ========== */
.page-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 24px;
  padding: 24px 28px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.06);
  position: relative;
  z-index: 10;
}

.page-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e);
  border-radius: 16px 16px 0 0;
}

.page-header h2 {
  color: #1a472a;
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.page-header p {
  color: #64748b;
  margin: 0;
  font-size: 14px;
}

/* ========== Profile Container ========== */
.profile-container {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 24px;
  position: relative;
  z-index: 10;
}

/* ========== Info Card ========== */
.info-card, .pwd-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 4px 20px rgba(26, 71, 42, 0.06);
}

.info-card h3, .pwd-card h3 {
  color: #1a472a;
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 700;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(71, 85, 99, 0.1);
  letter-spacing: -0.01em;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(71, 85, 99, 0.06);
}

.info-item:last-child {
  border-bottom: none;
}

.info-item label {
  font-size: 14px;
  color: #64748b;
  width: 80px;
  font-weight: 500;
}

.info-item span {
  font-size: 15px;
  color: #1a472a;
  font-weight: 600;
}

/* ========== Password Form ========== */
.pwd-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
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

/* 输入框样式 */
.form-input {
  width: 100%;
  padding: 16px 50px 16px 50px;
  border: 1px solid rgba(71, 85, 99, 0.2);
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  color: #1a472a;
  background: rgba(255, 255, 255, 0.8);
}

.form-input:focus {
  outline: none;
  border-color: #3a7d44;
  box-shadow: 0 0 0 4px rgba(58, 125, 68, 0.1);
  background: #fff;
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

.icon-lock::before {
  content: "🔒";
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

/* 密码显示/隐藏按钮 */
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

.eye-icon {
  font-size: 18px;
  font-style: normal;
  line-height: 1;
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .profile-page {
    padding: 16px;
  }

  .page-header {
    padding: 18px 20px;
  }

  .page-header h2 {
    font-size: 18px;
  }

  .profile-container {
    grid-template-columns: 1fr;
  }

  .info-card, .pwd-card {
    padding: 20px;
  }
}
</style>
