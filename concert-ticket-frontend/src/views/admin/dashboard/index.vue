<template>
  <div class="app-container" v-loading="loading">
    <h3>数据看板</h3>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="hover" style="margin-bottom:20px">
          <div style="text-align:center">
            <p style="color:#999;font-size:14px">{{ card.label }}</p>
            <h2 style="color:#409EFF;margin:10px 0">{{ card.value }}</h2>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <div slot="header"><span>收入报表</span></div>
      <el-form :inline="true" size="small">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchRevenue">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="revenueList" border>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="revenue" label="收入">
          <template slot-scope="{row}">¥{{ row.revenue }}</template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款">
          <template slot-scope="{row}">¥{{ row.refundAmount }}</template>
        </el-table-column>
        <el-table-column prop="ticketCount" label="售票数" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getSalesOverview, getRevenueReport } from '@/api/admin/dashboard'

export default {
  name: 'AdminDashboard',
  data() {
    return {
      loading: false,
      cards: [
        { label: '总订单数', value: 0, key: 'totalOrders' },
        { label: '总销售额', value: '¥0', key: 'totalRevenue' },
        { label: '活跃演唱会', value: 0, key: 'activeConcerts' },
        { label: '注册用户数', value: 0, key: 'totalUsers' }
      ],
      dateRange: [],
      revenueList: []
    }
  },
  created() {
    this.fetchSales()
  },
  methods: {
    fetchSales() {
      this.loading = true
      getSalesOverview().then(res => {
        const d = res.data
        this.cards[0].value = d.totalOrders || 0
        this.cards[1].value = '¥' + (d.totalRevenue || 0)
        this.cards[2].value = d.activeConcerts || 0
        this.cards[3].value = d.totalUsers || 0
      }).finally(() => { this.loading = false })
    },
    fetchRevenue() {
      if (!this.dateRange || this.dateRange.length !== 2) {
        this.$message.warning('请选择日期范围')
        return
      }
      getRevenueReport({ startDate: this.dateRange[0], endDate: this.dateRange[1] }).then(res => {
        this.revenueList = res.data || []
      })
    }
  }
}
</script>
