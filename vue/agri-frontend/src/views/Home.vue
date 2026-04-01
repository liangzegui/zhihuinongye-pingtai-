<template>
  <div class="home-page">
    <div class="hero">
      <div class="hero-text">
        <p class="badge">智慧农业 · 实时在线</p>
        <h1>一屏掌控农场环境与设备</h1>
        <p class="subtitle">温湿度、光照、土壤、CO₂ 全链路可视化，支持远程控制与阈值下发。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="navigateTo('/realtime')">查看实时数据</el-button>
          <el-button size="large" @click="navigateTo('/settings')">系统设置</el-button>
        </div>
      </div>
      <div class="hero-illustration">
        <div class="circle"></div>
        <div class="card ghost">土壤湿度 · 光照强度</div>
        <div class="card ghost">泵 / 风扇 / 照明 开关</div>
      </div>
    </div>

    <!-- ======== 实时数据概览 ======== -->
    <div class="dashboard-section">
      <h2 class="section-title">📡 环境概览</h2>
      <div class="dashboard-cards" v-loading="dashLoading">
        <div class="dash-card temp">
          <div class="dash-icon icon-temp">
            <img src="@/assets/thermometer.svg" alt="温度" class="dash-icon-img" />
          </div>
          <div class="dash-info">
            <span class="dash-value">{{ dashboard.temperature ?? '--' }}<small>°C</small></span>
            <span class="dash-label">温度</span>
          </div>
        </div>
        <div class="dash-card humi">
          <div class="dash-icon icon-humi">
            <img src="@/assets/humidity.svg" alt="湿度" class="dash-icon-img" />
          </div>
          <div class="dash-info">
            <span class="dash-value">{{ dashboard.humidity ?? '--' }}<small>%</small></span>
            <span class="dash-label">湿度</span>
          </div>
        </div>
        <div class="dash-card soil">
          <div class="dash-icon icon-soil">
            <img src="@/assets/soil.svg" alt="土壤" class="dash-icon-img" />
          </div>
          <div class="dash-info">
            <span class="dash-value">{{ dashboard.soilAdc ?? '--' }}<small>ADC</small></span>
            <span class="dash-label">土壤</span>
          </div>
        </div>
        <div class="dash-card light">
          <div class="dash-icon icon-light">
            <img src="@/assets/light.svg" alt="光照" class="dash-icon-img" />
          </div>
          <div class="dash-info">
            <span class="dash-value">{{ dashboard.lightIntensity ?? '--' }}<small>lux</small></span>
            <span class="dash-label">光照</span>
          </div>
        </div>
        <div class="dash-card co2">
          <div class="dash-icon icon-co2">
            <img src="@/assets/co2.svg" alt="CO2" class="dash-icon-img" />
          </div>
          <div class="dash-info">
            <span class="dash-value">{{ dashboard.co2 ?? '--' }}<small>ppm</small></span>
            <span class="dash-label">CO₂</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ======== 统计摘要 ======== -->
    <div class="quick-stats">
      <div class="stat-item">
        <div class="stat-value">{{ dashboard.todayDataCount ?? 0 }}</div>
        <div class="stat-label">今日采集</div>
      </div>
      <div class="stat-item">
        <div class="stat-value warning-num">{{ dashboard.unhandledWarnings ?? 0 }}</div>
        <div class="stat-label">未处理警告</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ dashboard.totalDataCount ?? 0 }}</div>
        <div class="stat-label">总数据量</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">10s</div>
        <div class="stat-label">上报周期</div>
      </div>
    </div>

    <!-- ======== 功能导航 ======== -->
    <div class="feature-cards">
      <div class="card" @click="navigateTo('/realtime')">
        <div class="card-icon">📊</div>
        <h3>实时环境数据</h3>
        <p>温湿度、光照、土壤湿度、CO₂ 一览</p>
        <span class="pill">秒级刷新</span>
      </div>
      <div class="card" @click="navigateTo('/historical')">
        <div class="card-icon">📈</div>
        <h3>历史数据查询</h3>
        <p>分页查询并导出，辅助溯源分析</p>
        <span class="pill">曲线对比</span>
      </div>
      <div class="card" @click="navigateTo('/analysis')">
        <div class="card-icon">🔍</div>
        <h3>数据趋势分析</h3>
        <p>多维度趋势与异常识别</p>
        <span class="pill">趋势洞察</span>
      </div>
      <div class="card" @click="navigateTo('/warning')">
        <div class="card-icon">⚠️</div>
        <h3>警告日志</h3>
        <p>设备与环境告警快速查看</p>
        <span class="pill">即时告警</span>
      </div>
      <div class="card" @click="navigateTo('/profile')">
        <div class="card-icon">👤</div>
        <h3>个人中心</h3>
        <p>账户、权限与偏好配置</p>
        <span class="pill">设置</span>
      </div>
    </div>
  </div>
</template>

<script>
import { getDashboardOverview } from '@/api/data'
import { getRealTimeData } from '@/api/data'
import { ElMessage } from 'element-plus'

export default {
  name: 'HomePage',
  data() {
    return {
      dashboard: {},
      dashLoading: false,
      refreshTimer: null
    }
  },
  mounted() {
    this.fetchDashboard()
    // 主页保持与实时页面一致：定时刷新最新环境数据
    this.refreshTimer = setInterval(() => {
      this.fetchDashboard(false)
    }, 5000)
  },
  beforeUnmount() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
      this.refreshTimer = null
    }
  },
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    },
    async fetchDashboard(showLoading = true) {
      if (showLoading) {
        this.dashLoading = true
      }
      try {
        const [dashboardRes, realtimeRes] = await Promise.all([
          getDashboardOverview(),
          getRealTimeData()
        ])

        const overview = dashboardRes?.data || {}

        // 兼容后端统一Result和数组/对象两种响应格式
        let realtimeData = null
        if (realtimeRes && realtimeRes.code === 200 && realtimeRes.data) {
          realtimeData = Array.isArray(realtimeRes.data) ? realtimeRes.data[0] : realtimeRes.data
        } else if (realtimeRes && Array.isArray(realtimeRes)) {
          realtimeData = realtimeRes[0]
        } else if (realtimeRes && realtimeRes.data) {
          realtimeData = Array.isArray(realtimeRes.data) ? realtimeRes.data[0] : realtimeRes.data
        }

        this.dashboard = {
          ...overview,
          temperature: realtimeData?.temperature ?? overview.temperature ?? null,
          humidity: realtimeData?.humidity ?? overview.humidity ?? null,
          soilAdc: realtimeData?.soilAdc ?? realtimeData?.soilMoisture ?? overview.soilAdc ?? null,
          lightIntensity: realtimeData?.lightIntensity ?? realtimeData?.light_intensity ?? overview.lightIntensity ?? null,
          co2: realtimeData?.co2 ?? overview.co2 ?? null
        }
      } catch (e) {
        console.error('[Home] 获取仪表盘数据失败:', e)
        if (showLoading) {
          ElMessage.error('获取仪表盘数据失败，请检查网络连接')
        }
      } finally {
        if (showLoading) {
          this.dashLoading = false
        }
      }
    }
  }
}
</script>

<style scoped>
/* ========== 智慧农业主题设计 ========== */
/* Primary: #1a472a (深森林绿) Accent: #3a7d44 (森林绿)
   Secondary: #0f766e (青色) Surface: #f0fdf4 (薄荷绿) */

.home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 60px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 50%, #f0fdfa 100%);
  position: relative;
}

.home-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 50%;
  height: 50%;
  background: radial-gradient(ellipse at top left, rgba(58, 125, 68, 0.06) 0%, transparent 70%);
  pointer-events: none;
}

/* ========== Hero Section ========== */
.hero {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(240, 253, 244, 0.9) 100%);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(71, 85, 99, 0.1);
  border-radius: 20px;
  padding: 36px 40px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-items: center;
  gap: 28px;
  margin-bottom: 32px;
  box-shadow: 0 12px 40px rgba(26, 71, 42, 0.08);
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1a472a, #3a7d44, #22c55e, #0f766e);
  border-radius: 20px 20px 0 0;
}

.hero::after {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 60%;
  height: 150%;
  background: radial-gradient(ellipse, rgba(58, 125, 68, 0.06) 0%, transparent 60%);
  pointer-events: none;
}

.hero-text h1 {
  font-size: 2rem;
  color: #1a472a;
  margin: 14px 0;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.hero-text .subtitle {
  color: #64748b;
  margin: 0 0 20px;
  line-height: 1.7;
  font-size: 15px;
}

.badge {
  display: inline-block;
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba(58, 125, 68, 0.12), rgba(34, 197, 94, 0.08));
  color: #166534;
  border-radius: 999px;
  font-weight: 600;
  letter-spacing: 0.3px;
  font-size: 13px;
  border: 1px solid rgba(34, 197, 94, 0.15);
}

.hero-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

/* Hero区域按钮使用大尺寸 - 全局样式已统一，仅调整尺寸 */
.hero-actions :deep(.el-button) {
  padding: 12px 24px;
  min-height: 44px;
}

.hero-illustration {
  position: relative;
  min-height: 180px;
  z-index: 1;
}

.hero-illustration .circle {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(58, 125, 68, 0.15), rgba(58, 125, 68, 0));
}

.hero-illustration .card {
  position: absolute;
  right: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(71, 85, 99, 0.1);
  border-radius: 14px;
  padding: 14px 18px;
  box-shadow: 0 8px 24px rgba(26, 71, 42, 0.1);
  color: #1a472a;
  font-weight: 600;
  font-size: 14px;
}

.hero-illustration .card:nth-child(2) {
  top: 15px;
}

.hero-illustration .card:nth-child(3) {
  bottom: 15px;
}

/* ========== Dashboard Section ========== */
.dashboard-section {
  margin-bottom: 36px;
}

.section-title {
  color: #1a472a;
  font-size: 1.35rem;
  margin-bottom: 18px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.dash-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 22px 20px;
  box-shadow: 0 6px 24px rgba(26, 71, 42, 0.06);
  border: 1px solid rgba(71, 85, 99, 0.08);
  border-left: 4px solid #3a7d44;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dash-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(26, 71, 42, 0.12);
}

.dash-card.temp { border-color: #ef4444; }
.dash-card.humi { border-color: #14b8a6; }
.dash-card.soil { border-color: #3a7d44; }
.dash-card.light { border-color: #d97706; }
.dash-card.co2 { border-color: #0f766e; }

.dash-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  flex-shrink: 0;
}

.dash-icon-img {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.icon-temp { background: linear-gradient(135deg, rgba(239, 68, 68, 0.12), rgba(239, 68, 68, 0.06)); }
.icon-humi { background: linear-gradient(135deg, rgba(20, 184, 166, 0.12), rgba(20, 184, 166, 0.06)); }
.icon-soil { background: linear-gradient(135deg, rgba(58, 125, 68, 0.12), rgba(58, 125, 68, 0.06)); }
.icon-light { background: linear-gradient(135deg, rgba(217, 119, 6, 0.12), rgba(217, 119, 6, 0.06)); }
.icon-co2 { background: linear-gradient(135deg, rgba(15, 118, 110, 0.12), rgba(15, 118, 110, 0.06)); }

.dash-info {
  display: flex;
  flex-direction: column;
}

.dash-value {
  font-size: 1.7rem;
  font-weight: 700;
  color: #1a472a;
  line-height: 1.2;
}

.dash-value small {
  font-size: 0.7rem;
  font-weight: 500;
  margin-left: 4px;
  color: #64748b;
}

.dash-label {
  font-size: 0.85rem;
  color: #64748b;
  margin-top: 4px;
}

/* ========== Quick Stats ========== */
.quick-stats {
  display: flex;
  justify-content: center;
  gap: 60px;
  flex-wrap: wrap;
  margin-bottom: 40px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px solid rgba(71, 85, 99, 0.08);
}

.stat-item {
  text-align: center;
}

.stat-value {
  color: #1a472a;
  font-size: 2.2rem;
  font-weight: 700;
  margin-bottom: 8px;
}

.stat-value.warning-num {
  color: #ea580c;
}

.stat-label {
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 500;
}

/* ========== Feature Cards ========== */
.feature-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 60px;
}

.card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 18px;
  padding: 28px 24px;
  box-shadow: 0 8px 28px rgba(26, 71, 42, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  border: 1px solid rgba(71, 85, 99, 0.08);
  overflow: hidden;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #1a472a, #3a7d44);
  opacity: 0;
  transition: opacity 0.3s;
}

.card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(26, 71, 42, 0.14);
  border-color: rgba(58, 125, 68, 0.3);
}

.card:hover::before {
  opacity: 1;
}

.card-icon {
  font-size: 2.8rem;
  margin-bottom: 18px;
}

.card h3 {
  color: #1a472a;
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: -0.01em;
}

.card p {
  color: #64748b;
  line-height: 1.6;
  margin: 0;
  font-size: 14px;
}

.pill {
  display: inline-block;
  margin-top: 16px;
  padding: 7px 14px;
  background: linear-gradient(135deg, rgba(58, 125, 68, 0.1), rgba(34, 197, 94, 0.06));
  color: #166534;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  border: 1px solid rgba(34, 197, 94, 0.12);
}

/* ========== Responsive ========== */
@media (max-width: 768px) {
  .home-page {
    padding: 20px 16px;
  }

  .hero {
    padding: 28px 24px;
  }

  .hero-text h1 {
    font-size: 1.5rem;
  }

  .feature-cards {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .quick-stats {
    gap: 32px;
    padding: 20px;
  }

  .stat-value {
    font-size: 1.6rem;
  }

  .dashboard-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
