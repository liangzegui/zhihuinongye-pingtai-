/**
 * WebSocket服务
 * 用于接收后端推送的实时传感器数据和设备状态
 */
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

class WebSocketService {
  constructor() {
    this.stompClient = null
    this.connected = false
    this.subscriptions = {}
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
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

    return new Promise((resolve, reject) => {
      try {
        // 获取后端地址
        const wsUrl = (baseUrl || this.getBaseUrl()) + '/ws'
        console.log('[WebSocket] 正在连接:', wsUrl)

        // 使用SockJS创建连接
        const socket = new SockJS(wsUrl)
        this.stompClient = Stomp.over(socket)

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
            this.reconnectAttempts = 0
            this.notifyConnectionStatus(true)
            
            // 订阅主题
            this.subscribeToTopics()
            resolve()
          },
          (error) => {
            console.error('[WebSocket] 连接错误:', error)
            this.connected = false
            this.notifyConnectionStatus(false)
            this.attemptReconnect()
            reject(error)
          }
        )

        // 连接关闭回调
        socket.onclose = () => {
          console.log('[WebSocket] 连接关闭')
          this.connected = false
          this.notifyConnectionStatus(false)
          this.attemptReconnect()
        }

      } catch (error) {
        console.error('[WebSocket] 创建连接失败:', error)
        reject(error)
      }
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
      console.log('[WebSocket] 已达到最大重连次数')
      return
    }

    this.reconnectAttempts++
    console.log(`[WebSocket] ${this.reconnectDelay / 1000}秒后尝试第${this.reconnectAttempts}次重连...`)

    setTimeout(() => {
      this.connect().catch(() => {})
    }, this.reconnectDelay)
  }

  /**
   * 获取后端基础URL
   */
  getBaseUrl() {
    // 优先使用环境变量
    if (process.env.VUE_APP_API_BASE_URL) {
      return process.env.VUE_APP_API_BASE_URL.replace('/api', '')
    }
    // 默认使用当前页面的源
    return window.location.origin
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
}

// 导出单例
export default new WebSocketService()
