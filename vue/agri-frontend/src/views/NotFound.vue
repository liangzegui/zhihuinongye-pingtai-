<template>
  <div class="not-found-page">
    <div class="content">
      <div class="code">404</div>
      <h2>页面未找到</h2>
      <p>你访问的页面不存在或已被移除</p>
      <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default { name: 'NotFound' }
</script>

<style scoped>
.not-found-page {
  display: flex; align-items: center; justify-content: center;
  min-height: 80vh; text-align: center;
}
.code {
  font-size: 6rem; font-weight: 800; color: #c8e6c9;
  line-height: 1; margin-bottom: 12px;
}
h2 { color: #2e7d32; margin-bottom: 8px; }
p { color: #666; margin-bottom: 24px; }
</style>
