<template>
  <div class="home-page">
    <div class="hero">
      <div class="hero-text">
        <p class="badge">智慧农业 · 实时在线</p>
        <h1>一屏掌控农场环境与设备</h1>
        <p class="subtitle">温湿度、光照、土壤、CO₂ 全链路可视化，支持远程控制与阈值下发。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="navigateTo('/realtime')">查看实时数据</el-button>
          <el-button size="large" @click="navigateTo('/realtime')">设备控制</el-button>
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
.home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px 60px;
}

.hero {
  background: radial-gradient(circle at 20% 20%, rgba(66, 185, 131, 0.25), transparent 35%),
              radial-gradient(circle at 80% 0%, rgba(52, 152, 219, 0.22), transparent 45%),
              linear-gradient(135deg, #f7fdf8 0%, #f0f7ff 100%);
  border: 1px solid #e2f0e8;
  border-radius: 18px;
  padding: 32px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
  box-shadow: 0 12px 36px rgba(0, 128, 96, 0.08);
}

.hero-text h1 { font-size: 2.2rem; color: #1f3b2d; margin: 12px 0; line-height: 1.2; }
.hero-text .subtitle { color: #47624f; margin: 0 0 16px; line-height: 1.6; }

.badge {
  display: inline-block; padding: 6px 12px;
  background: rgba(46, 204, 113, 0.12); color: #2e7d32;
  border-radius: 999px; font-weight: 600; letter-spacing: 0.3px;
}

.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; }

.hero-illustration { position: relative; min-height: 180px; }
.hero-illustration .circle {
  position: absolute; inset: 0; margin: auto; width: 200px; height: 200px;
  border-radius: 50%; background: radial-gradient(circle, rgba(46, 204, 113, 0.2), rgba(46, 204, 113, 0));
}
.hero-illustration .card {
  position: absolute; right: 0; background: white; border: 1px solid #e6f0ea;
  border-radius: 12px; padding: 12px 14px; box-shadow: 0 12px 24px rgba(0,0,0,0.06);
  color: #2e7d32; font-weight: 600;
}
.hero-illustration .card:nth-child(2) { top: 20px; }
.hero-illustration .card:nth-child(3) { bottom: 20px; }

/* ========== 仪表盘概览 ========== */
.dashboard-section { margin-bottom: 36px; }
.section-title { color: #1b5e20; font-size: 1.3rem; margin-bottom: 16px; }

.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.dash-card {
  display: flex; align-items: center; gap: 14px;
  background: white; border-radius: 14px; padding: 20px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.06);
  border-left: 4px solid #4caf50;
  transition: transform .2s;
}
.dash-card:hover { transform: translateY(-3px); }
.dash-card.temp  { border-color: #ef5350; }
.dash-card.humi  { border-color: #42a5f5; }
.dash-card.soil  { border-color: #8d6e63; }
.dash-card.light { border-color: #ffa726; }
.dash-card.co2   { border-color: #7e57c2; }

.dash-icon {
  width: 48px; height: 48px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; flex-shrink: 0;
}
.dash-icon-img {
  width: 28px; height: 28px; object-fit: contain;
}
.icon-temp { background: rgba(239,83,80,0.12); }
.icon-humi { background: rgba(66,165,245,0.12); }
.icon-soil { background: rgba(141,110,99,0.12); }
.icon-light { background: rgba(255,167,38,0.12); }
.icon-co2 { background: rgba(126,87,194,0.12); }
.dash-info { display: flex; flex-direction: column; }
.dash-value { font-size: 1.6rem; font-weight: 700; color: #1b5e20; }
.dash-value small { font-size: 0.75rem; font-weight: 400; margin-left: 3px; color: #666; }
.dash-label { font-size: 0.85rem; color: #888; margin-top: 2px; }

/* ========== 统计摘要 ========== */
.quick-stats {
  display: flex; justify-content: center; gap: 60px; flex-wrap: wrap;
  margin-bottom: 40px;
}
.stat-item { text-align: center; }
.stat-value { color: #2e7d32; font-size: 2rem; font-weight: 700; margin-bottom: 8px; }
.stat-value.warning-num { color: #e65100; }
.stat-label { color: #558b2f; font-size: 0.9rem; opacity: 0.8; }

/* ========== 功能卡片 ========== */
.feature-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px; margin-bottom: 60px;
}
.card {
  background: white; border-radius: 14px; padding: 26px 22px;
  box-shadow: 0 10px 26px rgba(0,0,0,0.06);
  transition: transform .25s ease, box-shadow .25s ease, border-color .25s ease;
  cursor: pointer; position: relative; border: 1px solid #e8f5e9;
}
.card:hover { transform: translateY(-6px); box-shadow: 0 14px 32px rgba(46,125,50,0.15); border-color: #4caf50; }
.card-icon { font-size: 2.5rem; margin-bottom: 16px; }
.card h3 { color: #2e7d32; font-size: 1.3rem; font-weight: 600; margin-bottom: 12px; }
.card p { color: #47624f; line-height: 1.5; margin: 6px 0 0; opacity: 0.85; }
.pill {
  display: inline-block; margin-top: 14px; padding: 6px 10px;
  background: rgba(76,175,80,0.12); color: #2e7d32;
  border-radius: 999px; font-size: 0.85rem; font-weight: 600;
}

@media (max-width: 768px) {
  .home-page { padding: 20px 16px; }
  .hero-text h1 { font-size: 1.6rem; }
  .feature-cards { grid-template-columns: 1fr; gap: 16px; }
  .quick-stats { gap: 30px; }
  .stat-value { font-size: 1.5rem; }
  .dashboard-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
