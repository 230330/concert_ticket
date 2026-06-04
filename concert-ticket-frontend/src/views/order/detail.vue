<template>
  <div class="order-detail-page" v-loading="loading">
    <template v-if="order">
      <!-- 订单状态进度条 -->
      <el-card class="status-card" shadow="never">
        <div class="status-progress">
          <div
            v-for="(step, index) in statusSteps"
            :key="index"
            :class="['step', { active: index <= currentStepIndex, current: index === currentStepIndex }]"
          >
            <div class="step-icon">
              <i :class="step.icon"></i>
            </div>
            <div class="step-label">{{ step.label }}</div>
          </div>
        </div>
        <div class="status-notice" v-if="order.status === 0">
          <el-alert
            :title="'请在 ' + remainTime + ' 内完成支付，超时订单将自动取消'"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>
      </el-card>

      <!-- 演唱会信息卡片 -->
      <el-card class="info-card" shadow="never">
        <div class="concert-info">
          <div class="concert-name">{{ order.concertName || '演唱会' }}</div>
          <div class="concert-meta">
            <span><i class="el-icon-location"></i> {{ order.venueName || '-' }}</span>
            <span><i class="el-icon-time"></i> {{ formatTime(order.showTime) }}</span>
          </div>
        </div>
      </el-card>

      <!-- 取票码卡片（已支付时显示） -->
      <el-card class="pickup-card" shadow="never" v-if="order.pickupCode">
        <div class="pickup-content">
          <div class="pickup-label">取票码</div>
          <div class="pickup-code">{{ order.pickupCode }}</div>
          <el-button type="text" size="small" @click="copyCode" style="margin-top:8px">
            <i class="el-icon-document-copy"></i> 复制取票码
          </el-button>
        </div>
      </el-card>

      <!-- 订单信息 -->
      <el-card class="detail-card" shadow="never">
        <div slot="header"><span>订单信息</span></div>
        <el-descriptions :column="2" border size="medium">
          <el-descriptions-item label="订单编号">
            <span class="order-no">{{ order.orderNo }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusTagType(order.status)" effect="dark">{{ statusText(order.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="amount">¥{{ order.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatTime(order.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ formatTime(order.expireTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 座位信息 -->
      <el-card class="detail-card" shadow="never" v-if="order.seats && order.seats.length > 0">
        <div slot="header"><span>座位信息</span></div>
        <div class="seat-list">
          <div v-for="seat in order.seats" :key="seat.seatId" class="seat-item">
            <div class="seat-main">
              <span class="seat-area">{{ seat.areaName }}</span>
              <span class="seat-no">{{ seat.seatNo }}</span>
            </div>
            <div class="seat-meta">
              <span class="seat-type">{{ seat.ticketTypeName }}</span>
              <span class="seat-price">¥{{ seat.price }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button @click="$router.back()">返回</el-button>
        <el-button v-if="order.status === 0" type="success" size="medium" @click="handlePay">立即支付</el-button>
        <el-button v-if="order.status === 0" type="danger" size="medium" plain @click="handleCancel">取消订单</el-button>
        <el-button v-if="order.status === 1" type="warning" size="medium" plain @click="handleRefund">申请退款</el-button>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script>
import { getOrderDetail, payOrder, cancelOrder, refundOrder } from '@/api/order'

const STATUS_MAP = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' }
const TAG_MAP = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'success' }
const STATUS_STEPS_MAP = {
  0: [
    { label: '提交订单', icon: 'el-icon-edit' },
    { label: '支付订单', icon: 'el-icon-bank-card' },
    { label: '购票成功', icon: 'el-icon-circle-check' }
  ],
  1: [
    { label: '提交订单', icon: 'el-icon-edit' },
    { label: '支付成功', icon: 'el-icon-bank-card' },
    { label: '取票入场', icon: 'el-icon-circle-check' }
  ],
  4: [
    { label: '提交订单', icon: 'el-icon-edit' },
    { label: '支付成功', icon: 'el-icon-bank-card' },
    { label: '已完成', icon: 'el-icon-circle-check' }
  ],
  2: [
    { label: '提交订单', icon: 'el-icon-edit' },
    { label: '已取消', icon: 'el-icon-circle-close' }
  ],
  3: [
    { label: '提交订单', icon: 'el-icon-edit' },
    { label: '支付成功', icon: 'el-icon-bank-card' },
    { label: '已退款', icon: 'el-icon-circle-close' }
  ]
}
const STEP_INDEX_MAP = { 0: 0, 1: 2, 2: 1, 3: 2, 4: 2 }

export default {
  name: 'OrderDetail',
  data() {
    return {
      order: null,
      loading: false,
      remainTime: '',
      timer: null
    }
  },
  computed: {
    statusSteps() {
      return STATUS_STEPS_MAP[this.order ? this.order.status : 0] || STATUS_STEPS_MAP[0]
    },
    currentStepIndex() {
      return STEP_INDEX_MAP[this.order ? this.order.status : 0] || 0
    }
  },
  created() {
    this.fetchDetail()
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    statusText(s) { return STATUS_MAP[s] || '未知' },
    statusTagType(s) { return TAG_MAP[s] || 'info' },
    formatTime(t) {
      if (!t) return '-'
      return t.replace('T', ' ').substring(0, 19)
    },
    fetchDetail() {
      this.loading = true
      getOrderDetail(this.$route.params.id).then(res => {
        this.order = res.data
        // 如果是待支付订单，启动倒计时
        if (this.order && this.order.status === 0) {
          this.startCountdown()
        }
      }).finally(() => { this.loading = false })
    },
    startCountdown() {
      if (this.timer) clearInterval(this.timer)
      this.updateRemainTime()
      this.timer = setInterval(() => {
        this.updateRemainTime()
      }, 1000)
    },
    updateRemainTime() {
      if (!this.order || !this.order.expireTime) return
      const expire = new Date(this.order.expireTime.replace('T', ' '))
      const now = new Date()
      const diff = expire - now
      if (diff <= 0) {
        this.remainTime = '已过期'
        if (this.timer) clearInterval(this.timer)
        return
      }
      const min = Math.floor(diff / 60000)
      const sec = Math.floor((diff % 60000) / 1000)
      this.remainTime = `${min}分${sec}秒`
    },
    copyCode() {
      if (!this.order || !this.order.pickupCode) return
      navigator.clipboard.writeText(this.order.pickupCode).then(() => {
        this.$message.success('取票码已复制')
      }).catch(() => {
        // fallback for older browsers
        const input = document.createElement('input')
        input.value = this.order.pickupCode
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('取票码已复制')
      })
    },
    handlePay() {
      this.$confirm('确认支付该订单？', '支付确认', {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        payOrder({ orderId: this.order.id }).then(() => {
          this.$message.success('支付成功')
          if (this.timer) clearInterval(this.timer)
          this.fetchDetail()
        })
      }).catch(() => {})
    },
    handleCancel() {
      this.$confirm('确认取消该订单？取消后座位将释放。', '取消确认', {
        confirmButtonText: '确认取消',
        cancelButtonText: '再想想',
        type: 'warning'
      }).then(() => {
        cancelOrder({ orderId: this.order.id }).then(() => {
          this.$message.success('订单已取消')
          if (this.timer) clearInterval(this.timer)
          this.fetchDetail()
        })
      }).catch(() => {})
    },
    handleRefund() {
      this.$confirm('确认申请退款？演出前48小时内不可退款。', '退款确认', {
        confirmButtonText: '确认退款',
        cancelButtonText: '再想想',
        type: 'warning'
      }).then(() => {
        refundOrder({ orderId: this.order.id }).then(() => {
          this.$message.success('退款成功')
          this.fetchDetail()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.order-detail-page {
  max-width: 700px;
  margin: 20px auto;
  padding: 0 20px;
}

.status-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.status-progress {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 10px 0;

  .step {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    flex: 1;

    &:not(:last-child)::after {
      content: '';
      position: absolute;
      top: 20px;
      left: 55%;
      width: 90%;
      height: 2px;
      background: #e4e7ed;
    }

    &.active:not(:last-child)::after {
      background: #409eff;
    }

    .step-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #e4e7ed;
      color: #c0c4cc;
      font-size: 18px;
      margin-bottom: 8px;
      position: relative;
      z-index: 1;
    }

    &.active .step-icon {
      background: #409eff;
      color: #fff;
    }

    &.current .step-icon {
      box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.2);
    }

    .step-label {
      font-size: 12px;
      color: #909399;
    }

    &.active .step-label {
      color: #409eff;
      font-weight: bold;
    }
  }
}

.status-notice {
  margin-top: 12px;
}

.info-card {
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .concert-info {
    color: #fff;
    padding: 8px 0;

    .concert-name {
      font-size: 20px;
      font-weight: bold;
      margin-bottom: 10px;
    }

    .concert-meta {
      display: flex;
      gap: 20px;
      font-size: 14px;
      opacity: 0.9;

      i { margin-right: 4px; }
    }
  }
}

.pickup-card {
  margin-bottom: 16px;
  border-radius: 12px;
  background: #f0f9eb;
  border: 2px dashed #67c23a;

  .pickup-content {
    text-align: center;
    padding: 16px 0;

    .pickup-label {
      color: #67c23a;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .pickup-code {
      font-size: 36px;
      font-weight: bold;
      color: #303133;
      letter-spacing: 6px;
      font-family: 'Courier New', monospace;
    }
  }
}

.detail-card {
  margin-bottom: 16px;
  border-radius: 12px;

  .order-no {
    font-family: 'Courier New', monospace;
    font-size: 13px;
    color: #606266;
  }

  .amount {
    color: #f56c6c;
    font-size: 18px;
    font-weight: bold;
  }
}

.seat-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.seat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #409eff;

  .seat-main {
    display: flex;
    align-items: center;
    gap: 10px;

    .seat-area {
      font-weight: bold;
      color: #303133;
    }

    .seat-no {
      color: #606266;
      font-family: 'Courier New', monospace;
    }
  }

  .seat-meta {
    display: flex;
    align-items: center;
    gap: 12px;

    .seat-type {
      color: #909399;
      font-size: 13px;
    }

    .seat-price {
      color: #f56c6c;
      font-weight: bold;
      font-size: 16px;
    }
  }
}

.action-bar {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 24px 0;
}

@media (max-width: 768px) {
  .order-detail-page { padding: 0 10px; }
  .info-card .concert-info .concert-name { font-size: 16px; }
  .pickup-card .pickup-content .pickup-code { font-size: 28px; }
}
</style>
