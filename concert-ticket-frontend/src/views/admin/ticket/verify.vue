<template>
  <div class="verify-page">
    <el-card shadow="never" class="verify-card">
      <div slot="header">
        <span style="font-size:18px;font-weight:bold">取票码核销</span>
      </div>

      <div class="verify-input-section">
        <el-input
          v-model="pickupCode"
          placeholder="请输入取票码"
          size="large"
          clearable
          prefix-icon="el-icon-ticket"
          class="code-input"
          @keyup.enter.native="handleVerify"
        />
        <el-button
          type="primary"
          size="medium"
          icon="el-icon-check"
          :loading="verifying"
          :disabled="!pickupCode.trim()"
          @click="handleVerify"
          class="verify-btn"
        >
          核销
        </el-button>
      </div>

      <!-- 核销结果 -->
      <div v-if="result !== null" class="verify-result">
        <el-alert
          :title="result ? '核销成功！取票码已验证通过' : '核销失败'"
          :type="result ? 'success' : 'error'"
          :description="result ? '订单已标记为已完成状态' : '取票码无效或已使用，请确认后重试'"
          show-icon
          :closable="false"
        />
      </div>

      <!-- 操作说明 -->
      <div class="verify-tips">
        <h4>操作说明</h4>
        <ul>
          <li>输入用户出示的8位取票码</li>
          <li>点击"核销"按钮验证取票码</li>
          <li>核销成功后订单状态将自动变更为"已完成"</li>
          <li>每个取票码仅可核销一次</li>
        </ul>
      </div>
    </el-card>
  </div>
</template>

<script>
import { verifyTicketCode } from '@/api/ticket'

export default {
  name: 'AdminTicketVerify',
  data() {
    return {
      pickupCode: '',
      verifying: false,
      result: null
    }
  },
  methods: {
    handleVerify() {
      if (!this.pickupCode.trim()) return
      this.verifying = true
      this.result = null
      verifyTicketCode(this.pickupCode.trim()).then(res => {
        this.result = true
        this.$message.success('核销成功')
        this.pickupCode = ''
      }).catch(() => {
        this.result = false
      }).finally(() => {
        this.verifying = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.verify-page {
  max-width: 600px;
  margin: 20px auto;
  padding: 0 20px;
}

.verify-card {
  border-radius: 12px;
}

.verify-input-section {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;

  .code-input {
    flex: 1;
  }

  .verify-btn {
    min-width: 100px;
    border-radius: 8px;
  }
}

.verify-result {
  margin-bottom: 24px;
}

.verify-tips {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px 20px;

  h4 {
    margin: 0 0 10px 0;
    font-size: 14px;
    color: #606266;
  }

  ul {
    margin: 0;
    padding-left: 20px;

    li {
      font-size: 13px;
      color: #909399;
      line-height: 1.8;
    }
  }
}
</style>
