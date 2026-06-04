<template>
  <div class="ticket-page" v-loading="loading">
    <el-card class="page-card" shadow="never">
      <div slot="header" class="page-header">
        <span class="page-title">我的取票码</span>
        <el-button type="text" icon="el-icon-refresh" @click="fetchData">刷新</el-button>
      </div>

      <div v-if="ticketList.length > 0" class="ticket-list">
        <div v-for="item in ticketList" :key="item.id" class="ticket-card">
          <div class="ticket-main">
            <div class="ticket-code-section">
              <div class="ticket-label">取票码</div>
              <div class="ticket-code">{{ item.pickupCode }}</div>
              <el-button type="text" size="mini" @click="copyCode(item.pickupCode)">
                <i class="el-icon-document-copy"></i> 复制
              </el-button>
            </div>
            <div class="ticket-info">
              <div class="info-row">
                <span class="info-label">订单编号</span>
                <span class="info-value order-no">{{ item.orderNo }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">订单金额</span>
                <span class="info-value price">¥{{ item.totalAmount }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">下单时间</span>
                <span class="info-value">{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
          </div>
          <div class="ticket-status">
            <el-tag :type="item.status === 4 ? 'success' : 'warning'" effect="dark" size="small">
              {{ item.status === 4 ? '已取票' : '待取票' }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无取票码">
        <el-button type="primary" size="small" @click="$router.push('/concert/list')">去看看演出</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script>
import { getMyTicketCodes } from '@/api/ticket'

export default {
  name: 'MyTicketCodes',
  data() {
    return {
      ticketList: [],
      loading: false
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    formatTime(t) {
      if (!t) return '-'
      return t.replace('T', ' ').substring(0, 19)
    },
    fetchData() {
      this.loading = true
      getMyTicketCodes().then(res => {
        this.ticketList = res.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    copyCode(code) {
      navigator.clipboard.writeText(code).then(() => {
        this.$message.success('取票码已复制')
      }).catch(() => {
        const input = document.createElement('input')
        input.value = code
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('取票码已复制')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.ticket-page {
  max-width: 700px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-card {
  border-radius: 12px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .page-title {
    font-size: 18px;
    font-weight: bold;
  }
}

.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ticket-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 12px;
  border-left: 4px solid #409eff;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.ticket-main {
  flex: 1;
  display: flex;
  gap: 24px;
  align-items: flex-start;

  .ticket-code-section {
    text-align: center;
    min-width: 120px;

    .ticket-label {
      font-size: 12px;
      color: #909399;
      margin-bottom: 4px;
    }

    .ticket-code {
      font-size: 24px;
      font-weight: bold;
      color: #303133;
      font-family: 'Courier New', monospace;
      letter-spacing: 2px;
    }
  }

  .ticket-info {
    flex: 1;

    .info-row {
      display: flex;
      justify-content: space-between;
      padding: 4px 0;
      font-size: 13px;

      .info-label { color: #909399; }

      .info-value {
        color: #606266;

        &.order-no { font-family: 'Courier New', monospace; font-size: 12px; }
        &.price { color: #f56c6c; font-weight: bold; }
      }
    }
  }
}

.ticket-status {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .ticket-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .ticket-main {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
