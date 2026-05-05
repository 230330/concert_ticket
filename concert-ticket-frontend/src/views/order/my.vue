<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>我的订单</span>
        <el-select v-model="statusFilter" placeholder="订单状态" size="small" style="float:right;width:150px;margin-left:10px" clearable @change="fetchData">
          <el-option label="全部" :value="null" />
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已退款" :value="3" />
          <el-option label="已完成" :value="4" />
        </el-select>
      </div>

      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="orderNo" label="订单编号" width="180" show-overflow-tooltip />
        <el-table-column prop="concertName" label="演唱会" min-width="150" />
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template slot-scope="{row}">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template slot-scope="{row}">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="$router.push(`/order/detail/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 0" type="text" size="small" style="color:#67C23A" @click="handlePay(row)">支付</el-button>
            <el-button v-if="row.status === 0" type="text" size="small" style="color:#F56C6C" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="row.status === 1" type="text" size="small" style="color:#E6A23C" @click="handleRefund(row)">退款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        style="margin-top:20px;text-align:right"
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
      this.$confirm('确认支付该订单？', '提示', { type: 'info' }).then(() => {
        payOrder({ orderId: row.id }).then(() => {
          this.$message.success('支付成功')
          this.fetchData()
        })
      }).catch(() => {})
    },
    handleCancel(row) {
      this.$confirm('确认取消该订单？', '提示', { type: 'warning' }).then(() => {
        cancelOrder({ orderId: row.id }).then(() => {
          this.$message.success('已取消')
          this.fetchData()
        })
      }).catch(() => {})
    },
    handleRefund(row) {
      this.$confirm('确认退款该订单？', '提示', { type: 'warning' }).then(() => {
        refundOrder({ orderId: row.id }).then(() => {
          this.$message.success('退款成功')
          this.fetchData()
        })
      }).catch(() => {})
    }
  }
}
</script>
