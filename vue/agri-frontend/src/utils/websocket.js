/* eslint-disable */
/**
 * WebSocket服务
 * 用于接收后端推送的实时传感器数据和设备状态
 */
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'
import { getEsp32Config } from '@/api/config'
import { ElMessage } from 'element-plus'

class WebSocketService {
  constructor() {
    this.stompClient = null
    this.connected = false
    this.connecting = false
    this.subscriptions = {}
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.lastBaseUrl = ''
    this.esp32BaseUrl = '' // ESP32配置的URL
    this.listeners = {
      sensorData: [],
      deviceStatus: [],
      connectionStatus: [],
      warnings: []
    }
  }

  /**
   * 连接WebSocket服务器
   * @param {string} baseUrl - 后端地址，如 http://localhost:8080
   */
  connect(baseUrl = '') {
    if (this.connected) {
      console.log('[WebSocket] 已经连接')
      return Promise.resolve()
    }
    if (this.connecting) {
      console.log('[WebSocket] 正在连接中，跳过重复连接')
      return Promise.resolve()
    }

    this.connecting = true
    if (baseUrl) this.lastBaseUrl = baseUrl

    return new Promise((resolve, reject) => {
      const initConnection = (() => {
        this.getWebSocketUrl(baseUrl).then((wsUrl) => {
          console.log('[WebSocket] 正在连接:', wsUrl)

          // 使用SockJS创建连接（传入工厂函数以支持自动重连）
          this.stompClient = Stomp.over(() => new SockJS(wsUrl))

          // 关闭调试日志（生产环境）
          this.stompClient.debug = (msg) => {
            if (process.env.NODE_ENV === 'development') {
              console.log('[STOMP]', msg)
            }
          }

          this.stompClient.connect(
            {}, // 无需认证头
            () => {
              console.log('[WebSocket] 连接成功')
              this.connected = true
              this.connecting = false
              this.reconnectAttempts = 0
              this.notifyConnectionStatus(true)

              // 订阅主题
              this.subscribeToTopics()
              resolve()
            },
            () => {
              console.error('[WebSocket] 连接错误')
              this.connected = false
              this.connecting = false
              this.notifyConnectionStatus(false)
              this.attemptReconnect()
              reject(new Error('WebSocket连接失败'))
            }
          )

        }).catch((error) => {
          console.error('[WebSocket] 创建连接失败:', error)
          this.connecting = false
          reject(error)
        })
      })

      // 调用初始化函数
      initConnection()
    })
  }

  /**
   * 订阅消息主题
   */
  subscribeToTopics() {
    if (!this.stompClient || !this.connected) return

    // 订阅传感器数据
    this.subscriptions.sensorData = this.stompClient.subscribe(
      '/topic/sensor-data',
      (message) => {
        try {
          const data = JSON.parse(message.body)
          console.log('[WebSocket] 收到传感器数据:', data)
          this.notifySensorData(data)
        } catch (e) {
          console.error('[WebSocket] 解析传感器数据失败:', e)
        }
      }
    )

    // 订阅设备状态
    this.subscriptions.deviceStatus = this.stompClient.subscribe(
      '/topic/device-status',
      (message) => {
        try {
          const data = JSON.parse(message.body)
          console.log('[WebSocket] 收到设备状态:', data)
          this.notifyDeviceStatus(data)
        } catch (e) {
          console.error('[WebSocket] 解析设备状态失败:', e)
        }
      }
    )

    // 订阅预警通知
    this.subscriptions.warnings = this.stompClient.subscribe(
      '/topic/warnings',
      (message) => {
        try {
          const data = JSON.parse(message.body)
          console.log('[WebSocket] 收到预警通知:', data)
          this.notifyWarnings(data)
        } catch (e) {
          console.error('[WebSocket] 解析预警数据失败:', e)
        }
      }
    )

    console.log('[WebSocket] 已订阅主题: /topic/sensor-data, /topic/device-status, /topic/warnings')
  }

  /**
   * 断开连接
   */
  disconnect() {
    // 重置重连计数器
    this.reconnectAttempts = this.maxReconnectAttempts

    if (this.stompClient && this.connected) {
      // 取消所有订阅
      Object.values(this.subscriptions).forEach(sub => {
        if (sub) sub.unsubscribe()
      })
      this.subscriptions = {}

      this.stompClient.disconnect(() => {
        console.log('[WebSocket] 已断开连接')
        this.connected = false
        this.notifyConnectionStatus(false)
      })
    }
  }

  /**
   * 尝试重新连接
   */
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('[WebSocket] 已达到最大重连次数，停止重试')

      // 显示错误提示
      ElMessage.error({
        message: `WebSocket连接失败，已重试${this.maxReconnectAttempts}次。请检查网络连接或ESP32设备状态`,
        duration: 5000,
        showClose: true
      })

      // 通知连接失败
      this.notifyConnectionStatus(false)
      return
    }

    this.reconnectAttempts++
    const retryMsg = `第${this.reconnectAttempts}次重连`
    console.log(`[WebSocket] ${this.reconnectDelay / 1000}秒后尝试${retryMsg}...`)

    // 显示重连提示（仅前3次显示）
    if (this.reconnectAttempts <= 3) {
      ElMessage.warning({
        message: `连接断开，正在尝试${retryMsg}...`,
        duration: 2000
      })
    }

    setTimeout(() => {
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.connect(this.lastBaseUrl).catch(() => {})
      }
    }, this.reconnectDelay)
  }

  /**
   * 获取WebSocket连接URL
   */
  async getWebSocketUrl(customBaseUrl) {
    // 如果提供了自定义URL，直接使用
    if (customBaseUrl) {
      return customBaseUrl + '/ws'
    }

    // 注意：ESP32配置应该由后端服务器读取和使用
    // 前端始终连接到后端的WebSocket端点，由后端负责连接ESP32
    // 这样可以避免CORS问题，并且架构更清晰

    // 在开发环境下，始终使用相对路径通过Vue代理连接到后端
    // 在生产环境下，使用页面同源的WebSocket端点
    return this.getDefaultBaseUrl() + '/ws'
  }

  /**
   * 获取默认后端基础URL
   */
  getDefaultBaseUrl() {
    // 开发环境下直接连后端，避免与 webpack-dev-server 的 HMR WebSocket（/ws）路径冲突
    if (process.env.NODE_ENV === 'development') {
      const backendUrl = process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080'
      console.log('[WebSocket] 开发环境：直连后端 WebSocket:', backendUrl)
      return backendUrl
    }

    // 生产环境：优先使用环境变量
    if (process.env.VUE_APP_API_BASE_URL) {
      return process.env.VUE_APP_API_BASE_URL.replace('/api', '')
    }

    // 生产环境默认使用页面的源
    return window.location.origin
  }

  /**
   * 重置连接状态和重连计数器
   */
  resetConnection() {
    this.reconnectAttempts = 0
    this.connected = false
    this.connecting = false
  }

  // ========== 事件监听器管理 ==========

  /**
   * 添加传感器数据监听器
   * @param {Function} callback - 回调函数
   */
  onSensorData(callback) {
    if (typeof callback === 'function') {
      this.listeners.sensorData.push(callback)
    }
  }

  /**
   * 添加设备状态监听器
   * @param {Function} callback - 回调函数
   */
  onDeviceStatus(callback) {
    if (typeof callback === 'function') {
      this.listeners.deviceStatus.push(callback)
    }
  }

  /**
   * 添加连接状态监听器
   * @param {Function} callback - 回调函数
   */
  onConnectionStatus(callback) {
    if (typeof callback === 'function') {
      this.listeners.connectionStatus.push(callback)
    }
  }

  /**
   * 移除监听器
   * @param {string} type - 监听器类型
   * @param {Function} callback - 回调函数
   */
  removeListener(type, callback) {
    if (this.listeners[type]) {
      this.listeners[type] = this.listeners[type].filter(cb => cb !== callback)
    }
  }

  /**
   * 添加预警通知监听器
   * @param {Function} callback - 回调函数
   */
  onWarnings(callback) {
    if (typeof callback === 'function') {
      this.listeners.warnings.push(callback)
    }
  }

  // ========== 通知方法 ==========

  notifyWarnings(data) {
    this.listeners.warnings.forEach(cb => cb(data))
  }

  notifySensorData(data) {
    this.listeners.sensorData.forEach(cb => cb(data))
  }

  notifyDeviceStatus(data) {
    this.listeners.deviceStatus.forEach(cb => cb(data))
  }

  notifyConnectionStatus(connected) {
    this.listeners.connectionStatus.forEach(cb => cb(connected))
  }

  /**
   * 获取连接状态
   */
  isConnected() {
    return this.connected
  }

  /**
   * 获取当前使用的ESP32基础URL
   */
  getEsp32BaseUrl() {
    return this.esp32BaseUrl
  }

  /**
   * 获取重连状态信息
   */
  getReconnectInfo() {
    return {
      attempts: this.reconnectAttempts,
      maxAttempts: this.maxReconnectAttempts,
      isRetrying: this.reconnectAttempts > 0 && this.reconnectAttempts < this.maxReconnectAttempts
    }
  }
}

// 导出单例
export default new WebSocketService()