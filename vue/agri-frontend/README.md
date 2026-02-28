AGRI-FRONTEND/
├─ .vscode/                # VSCode配置文件（无需关注）
├─ dist/                   # 项目打包输出目录（自动生成）
├─ node_modules/           # 项目依赖包（npm install后生成）
├─ public/                 # 静态资源目录
│  ├─ favicon.ico          # 网站图标
│  └─ index.html           # 入口HTML文件
├─ src/                    # 源代码核心目录
│  ├─ api/                 # 接口请求封装目录（统一管理接口）
│  │  ├─ auth.js           # 登录/注册/验证码接口
│  │  ├─ data.js           # 实时/历史/趋势数据接口
│  │  ├─ warning.js        # 警告日志接口
│  │  └─ user.js           # 个人信息相关接口（预留）
│  ├─ assets/              # 静态资源（图片、样式等）
│  │  └─ logo.png          # 项目Logo
│  ├─ components/          # 公共组件目录（复用组件）
│  │  ├─ HelloWorld.vue    # 默认示例组件（可删除）
│  │  └─ Navbar.vue        # 全局导航栏组件（页面切换+退出登录）
│  ├─ router/              # 路由配置目录
│  │  └─ index.js          # 路由规则+登录守卫（控制页面访问权限）
│  ├─ utils/               # 工具函数目录
│  │  └─ request.js        # Axios请求实例（拦截器+基础配置）
│  ├─ views/               # 页面组件目录（核心功能页面）
│  │  ├─ Login.vue         # 登录页面（已完善）
│  │  ├─ Register.vue      # 注册页面（已完善）
│  │  ├─ RealTime.vue      # 实时环境数据页面
│  │  ├─ HistoricalData.vue # 历史数据分页查询页面
│  │  ├─ DataAnalysis.vue  # 数据趋势分析页面（ECharts可视化）
│  │  ├─ WarningLogs.vue   # 警告日志记录页面
│  │  └─ Personallnfo.vue  # 个人信息中心页面
│  ├─ App.vue              # 根组件（包含导航栏+路由出口）
│  └─ main.js              # 入口文件（初始化Vue实例+挂载路由）
├─ .browserslistrc         # 浏览器兼容配置
├─ .env.development        # 开发环境配置（后端地址、前端端口）
├─ .env.production         # 生产环境配置
├─ .eslintrc.js            # ESLint语法检查配置
├─ .gitignore              # Git忽略文件配置
├─ babel.config.js         # Babel编译配置
├─ jsconfig.json           # JS配置文件
├─ package-lock.json       # 依赖版本锁定文件
├─ package.json            # 项目配置文件（依赖+脚本）
├─ README.md               # 项目说明文档
└─ vue.config.js           # Vue项目配置文件（代理、端口等）

## 端口配置说明

本项目支持灵活配置前后端端口，只需修改 `.env.development` 文件即可：

```bash
# 后端 API 地址（修改此处即可改变后端端口）
VUE_APP_API_BASE_URL=http://localhost:8080

# 前端开发服务器端口
VUE_APP_DEV_PORT=8081
```

### 常见场景

1. **后端端口改为 9090**：
   ```bash
   VUE_APP_API_BASE_URL=http://localhost:9090
   ```

2. **前端端口改为 3000**：
   ```bash
   VUE_APP_DEV_PORT=3000
   ```

3. **后端在其他机器**：
   ```bash
   VUE_APP_API_BASE_URL=http://192.168.1.100:8080
   ```

修改后重启前端服务 `npm run serve` 即可生效。