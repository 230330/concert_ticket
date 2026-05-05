<template>
  <div class="app-container" v-loading="loading">
    <el-card v-if="order">
      <div slot="header">
        <span>订单详情</span>
        <el-tag :type="statusTagType(order.status)" style="float:right">{{ statusText(order.status) }}</el-tag>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ statusText(order.status) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">¥{{ order.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取票码">{{ order.pickupCode || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 座位信息 -->
      <div v-if="order.seats && order.seats.length > 0" style="margin-top:20px">
        <h4>座位信息</h4>
        <el-table :data="order.seats" border size="small" style="margin-top:10px">
          <el-table-column prop="areaName" label="区域" />
          <el-table-column prop="seatNo" label="座位号" />
          <el-table-column prop="ticketTypeName" label="票种" />
          <el-table-column prop="price" label="价格">
            <template slot-scope="{row}">¥{{ row.price }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div style="margin-top:20px">
        <el-button @click="$router.back()">返回</el-button>
        <el-button v-if="order.status === 0" type="success" @click="handlePay">立即支付</el-button>
        <el-button v-if="order.status === 0" type="danger" @click="handleCancel">取消订单</el-button>
        <el-button v-if="order.status === 1" type="warning" @click="handleRefund">申请退款</el-button>
      </div>
    </el-card>

    <el-empty v-else description="订单不存在" />
  </div>
</template>

<script>
import { getOrderDetail, payOrder, cancelOrder, refundOrder } from '@/api/order'

const STATUS_MAP = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' }
const TAG_MAP = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'success' }

export default {
  name: 'OrderDetail',
  data() {
    return {
      order: null,
      loading: false
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    statusText(s) { return STATUS_MAP[s] || '未知' },
    statusTagType(s) { return TAG_MAP[s] || 'info' },
    fetchDetail() {
      this.loading = true
      getOrderDetail(this.$route.params.id).then(res => {
        this.order = res.data
      }).finally(() => { this.loading = false })
    },
    handlePay() {
      payOrder({ orderId: this.order.id }).then(() => {
        this.$message.success('支付成功')
        this.fetchDetail()
      })
    },
    handleCancel() {
      this.$confirm('确认取消？', '提示', { type: 'warning' }).then(() => {
        cancelOrder({ orderId: this.order.id }).then(() => {
          this.$message.success('已取消')
          this.fetchDetail()
        })
      }).catch(() => {})
    },
    handleRefund() {
      this.$confirm('确认退款？', '提示', { type: 'warning' }).then(() => {
        refundOrder({ orderId: this.order.id }).then(() => {
          this.$message.success('退款成功')
          this.fetchDetail()
        })
      }).catch(() => {})
    }
  }
}
</script>
