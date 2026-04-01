import { defineStore } from 'pinia'

/**
 * 全局通知状态管理
 * 管理预警通知消息，支持未读计数和历史记录
 */
export const useNotificationStore = defineStore('notification', {
  state: () => ({
    /** 预警通知列表 */
    warnings: [],
    /** 未读数量 */
    unreadCount: 0
  }),

  getters: {
    /** 是否有未读通知 */
    hasUnread: (state) => state.unreadCount > 0,
    /** 最近的通知（最多10条） */
    recentWarnings: (state) => state.warnings.slice(0, 10)
  },

  actions: {
    /**
     * 添加新的预警通知
     * @param {Object} warning - 预警信息
     */
    addWarning(warning) {
      this.warnings.unshift({
        ...warning,
        _id: Date.now() + Math.random(),
        read: false,
        receivedTime: new Date().toLocaleString('zh-CN', { hour12: false })
      })
      this.unreadCount++
      // 只保留最近 50 条，同步修正未读计数
      if (this.warnings.length > 50) {
        const removed = this.warnings.slice(50)
        const removedUnread = removed.filter(w => !w.read).length
        this.warnings = this.warnings.slice(0, 50)
        this.unreadCount = Math.max(0, this.unreadCount - removedUnread)
      }
    },

    /**
     * 标记所有通知为已读
     */
    markAllRead() {
      this.warnings.forEach(w => { w.read = true })
      this.unreadCount = 0
    },

    /**
     * 清空所有通知
     */
    clearAll() {
      this.warnings = []
      this.unreadCount = 0
    }
  }
})
