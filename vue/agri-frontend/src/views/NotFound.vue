<template>
  <div class="not-found-page">
    <div class="content">
      <div class="code">404</div>
      <h2>页面未找到</h2>
      <p>你访问的页面不存在或已被移除</p>
      <el-button type="primary" size="large" @click="$router.push('/home')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default { name: 'NotFound' }
</script>

<style scoped>
/* ========== 智慧农业主题设计 ========== */

.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  text-align: center;
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.not-found-page::before {
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

.content {
  position: relative;
  z-index: 10;
  padding: 40px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  border: 1px solid rgba(71, 85, 99, 0.1);
  box-shadow: 0 12px 40px rgba(26, 71, 42, 0.1);
}

.code {
  font-size: 7rem;
  font-weight: 800;
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 16px;
  text-shadow: 0 4px 12px rgba(34, 197, 94, 0.2);
}

h2 {
  color: #1a472a;
  margin-bottom: 10px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

p {
  color: #64748b;
  margin-bottom: 28px;
  font-size: 15px;
}

/* 404页面按钮使用大尺寸 - 全局样式已统一，仅调整尺寸 */
:deep(.el-button) {
  padding: 14px 32px;
  min-height: 48px;
}
</style>
