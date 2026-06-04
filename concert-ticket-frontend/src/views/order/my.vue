<template>
  <div class="my-orders-page">
    <el-card class="page-card" shadow="never">
      <div slot="header" class="page-header">
        <span class="page-title">我的订单</span>
        <el-select v-model="statusFilter" placeholder="订单状态" size="small" style="width:140px" clearable @change="fetchData">
          <el-option label="全部" :value="null" />
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已退款" :value="3" />
          <el-option label="已完成" :value="4" />
        </el-select>
      </div>

      <div v-if="tableData.length > 0" class="order-list">
        <div v-for="order in tableData" :key="order.id" class="order-card" @click="$router.push(`/order/detail/${order.id}`)">
          <div class="order-top">
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <el-tag :type="statusTagType(order.status)" size="small" effect="dark">{{ statusText(order.status) }}</el-tag>
          </div>
          <div class="order-body">
            <div class="order-info">
              <div class="concert-name">{{ order.concertName || '演唱会' }}</div>
              <div class="order-meta">
                <span v-if="order.venueName"><i class="el-icon-location"></i> {{ order.venueName }}</span>
                <span><i class="el-icon-time"></i> {{ formatTime(order.showTime) }}</span>
              </div>
            </div>
            <div class="order-amount">
              <span class="amount-label">订单金额</span>
              <span class="amount-value">¥{{ order.totalAmount }}</span>
            </div>
          </div>
          <div class="order-bottom">
            <span class="create-time">下单时间：{{ formatTime(order.createTime) }}</span>
            <div class="order-actions" @click.stop>
              <el-button v-if="order.status === 0" type="primary" size="mini" round @click="handlePay(order)">立即支付</el-button>
              <el-button v-if="order.status === 0" size="mini" round @click="handleCancel(order)">取消</el-button>
              <el-button v-if="order.status === 1" type="warning" size="mini" round plain @click="handleRefund(order)">退款</el-button>
              <el-button type="text" size="small" @click="$router.push(`/order/detail/${order.id}`)">查看详情 ></el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else-if="!loading" description="暂无订单">
        <el-button type="primary" size="small" @click="$router.push('/concert/list')">去看看演出</el-button>
      </el-empty>

      <el-pagination
        v-if="total > 0"
        style="margin-top:20px;text-align:center"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="currentPage"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script>
import { getMyOrders, payOrder, cancelOrder, refundOrder } from '@/api/order'

const STATUS_MAP = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' }
const TAG_MAP = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'success' }

export default {
  name: 'MyOrders',
  data() {
    return {
      statusFilter: null,
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    statusText(status) { return STATUS_MAP[status] || '未知' },
    statusTagType(status) { return TAG_MAP[status] || 'info' },
    formatTime(t) {
      if (!t) return '-'
      return t.replace('T', ' ').substring(0, 16)
    },
    fetchData() {
      this.loading = true
      const params = { page: this.currentPage, size: this.pageSize }
      if (this.statusFilter !== null && this.statusFilter !== '') {
        params.status = this.statusFilter
      }
      getMyOrders(params).then(res => {
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handlePay(row) {
      this.$confirm('确认支付该订单？', '支付确认', { type: 'info' }).then(() => {
        payOrder({ orderId: row.id }).then(() => {
          this.$message.success('支付成功')
          this.fetchData()
        })
      }).catch(() => {})
    },
    handleCancel(row) {
      this.$confirm('确认取消该订单？取消后座位将释放。', '取消确认', { type: 'warning' }).then(() => {
        cancelOrder({ orderId: row.id }).then(() => {
          this.$message.success('已取消')
          this.fetchData()
        })
      }).catch(() => {})
    },
    handleRefund(row) {
      this.$confirm('确认退款？演出前48小时内不可退款。', '退款确认', { type: 'warning' }).then(() => {
        refundOrder({ orderId: row.id }).then(() => {
          this.$message.success('退款成功')
          this.fetchData()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.my-orders-page {
  max-width: 800px;
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

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  padding: 16px 20px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
  }
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .order-no {
    font-size: 13px;
    color: #909399;
    font-family: 'Courier New', monospace;
  }
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .concert-name {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 6px;
  }

  .order-meta {
    font-size: 13px;
    color: #909399;

    i { margin-right: 2px; }
    span + span { margin-left: 12px; }
  }

  .order-amount {
    text-align: right;

    .amount-label {
      font-size: 12px;
      color: #909399;
    }

    .amount-value {
      display: block;
      font-size: 22px;
      font-weight: bold;
      color: #f56c6c;
      margin-top: 4px;
    }
  }
}

.order-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;

  .create-time {
    font-size: 12px;
    color: #c0c4cc;
  }

  .order-actions {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

@media (max-width: 768px) {
  .order-body { flex-direction: column; align-items: flex-start; gap: 12px; }
  .order-bottom { flex-direction: column; align-items: flex-start; gap: 8px; }
}
</style>
