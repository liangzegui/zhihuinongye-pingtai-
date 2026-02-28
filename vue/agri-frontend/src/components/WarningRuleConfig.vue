<template>
  <div class="warning-rule-config">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="header-title">
            <el-icon><Bell /></el-icon>
            预警阈值规则管理
          </span>
          <el-button type="primary" size="small" @click="showAddDialog" icon="Plus">
            新增规则
          </el-button>
        </div>
      </template>

      <!-- 规则表格 -->
      <el-table :data="rules" stripe v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60" />

        <el-table-column label="传感器类型" width="130">
          <template #default="{ row }">
            <el-tag :type="getSensorTypeTag(row.sensorType)" size="small">
              {{ getSensorTypeText(row.sensorType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最小阈值" width="120">
          <template #default="{ row }">
            <span>{{ row.minValue !== null ? row.minValue : '-' }}</span>
            <span class="unit">{{ getUnit(row.sensorType) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="最大阈值" width="120">
          <template #default="{ row }">
            <span>{{ row.maxValue !== null ? row.maxValue : '-' }}</span>
            <span class="unit">{{ getUnit(row.sensorType) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              @change="handleToggle(row)"
              active-color="#67c23a"
              :loading="row._toggling"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="showEditDialog(row)">
              编辑
            </el-button>
            <el-popconfirm title="确定删除该规则？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small" text>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && rules.length === 0" description="暂无预警规则，请添加" />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="editingRule ? '编辑预警规则' : '新增预警规则'"
      v-model="dialogVisible"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="ruleForm" :rules="formRules" ref="ruleFormRef" label-width="100px">
        <el-form-item label="传感器类型" prop="sensorType">
          <el-select v-model="ruleForm.sensorType" placeholder="请选择" style="width: 100%;">
            <el-option label="温湿度" value="temp_hum" />
            <el-option label="土壤湿度" value="soil" />
            <el-option label="光照强度" value="light" />
            <el-option label="CO₂浓度" value="co2" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小阈值" prop="minValue">
          <el-input-number
            v-model="ruleForm.minValue"
            :precision="1"
            :step="1"
            placeholder="低于此值触发预警"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="最大阈值" prop="maxValue">
          <el-input-number
            v-model="ruleForm.maxValue"
            :precision="1"
            :step="1"
            placeholder="高于此值触发预警"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ editingRule ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getWarningRules, createWarningRule, updateWarningRule, deleteWarningRule, toggleWarningRule } from '@/api/warningRule'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'

export default {
  name: 'WarningRuleConfig',
  components: { Bell },
  data() {
    return {
      rules: [],
      loading: false,
      dialogVisible: false,
      editingRule: null,
      submitting: false,
      ruleForm: {
        sensorType: '',
        minValue: null,
        maxValue: null,
        enabled: 1
      },
      formRules: {
        sensorType: [{ required: true, message: '请选择传感器类型', trigger: 'change' }]
      }
    }
  },
  mounted() {
    this.fetchRules()
  },
  methods: {
    async fetchRules() {
      this.loading = true
      try {
        const res = await getWarningRules()
        this.rules = (res.data || []).map(r => ({ ...r, _toggling: false }))
      } catch (err) {
        console.error('获取预警规则失败:', err)
        ElMessage.error('获取预警规则失败')
      } finally {
        this.loading = false
      }
    },

    showAddDialog() {
      this.editingRule = null
      this.ruleForm = { sensorType: '', minValue: null, maxValue: null, enabled: 1 }
      this.dialogVisible = true
    },

    showEditDialog(rule) {
      this.editingRule = rule
      this.ruleForm = {
        sensorType: rule.sensorType,
        minValue: rule.minValue,
        maxValue: rule.maxValue,
        enabled: rule.enabled
      }
      this.dialogVisible = true
    },

    async handleSubmit() {
      const formRef = this.$refs.ruleFormRef
      if (!formRef) return

      try {
        await formRef.validate()
      } catch {
        return
      }

      // 校验阈值
      if (this.ruleForm.minValue !== null && this.ruleForm.maxValue !== null
          && this.ruleForm.minValue > this.ruleForm.maxValue) {
        ElMessage.warning('最小阈值不能大于最大阈值')
        return
      }

      this.submitting = true
      try {
        if (this.editingRule) {
          await updateWarningRule(this.editingRule.id, this.ruleForm)
          ElMessage.success('规则更新成功')
        } else {
          await createWarningRule(this.ruleForm)
          ElMessage.success('规则添加成功')
        }
        this.dialogVisible = false
        this.fetchRules()
      } catch (err) {
        console.error('保存规则失败:', err)
        ElMessage.error('保存失败: ' + (err.msg || err.message || '未知错误'))
      } finally {
        this.submitting = false
      }
    },

    async handleToggle(rule) {
      rule._toggling = true
      try {
        await toggleWarningRule(rule.id)
        rule.enabled = rule.enabled === 1 ? 0 : 1
        ElMessage.success(rule.enabled === 1 ? '规则已启用' : '规则已禁用')
      } catch (err) {
        console.error('切换状态失败:', err)
        ElMessage.error('操作失败')
      } finally {
        rule._toggling = false
      }
    },

    async handleDelete(id) {
      try {
        await deleteWarningRule(id)
        ElMessage.success('规则已删除')
        this.fetchRules()
      } catch (err) {
        console.error('删除规则失败:', err)
        ElMessage.error('删除失败')
      }
    },

    getSensorTypeText(type) {
      const map = { temp_hum: '温湿度', soil: '土壤湿度', light: '光照强度', co2: 'CO₂浓度' }
      return map[type] || type
    },

    getSensorTypeTag(type) {
      const map = { temp_hum: 'danger', soil: 'success', light: 'warning', co2: '' }
      return map[type] || 'info'
    },

    getUnit(type) {
      const map = { temp_hum: '°C/%', soil: 'ADC', light: 'lux', co2: 'ppm' }
      return map[type] || ''
    }
  }
}
</script>

<style scoped>
.warning-rule-config {
  max-width: 800px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 16px;
}

.unit {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}
</style>
