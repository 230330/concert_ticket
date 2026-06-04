<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 数据概览卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.key">
        <div class="stat-card" :style="{ borderTopColor: card.color }">
          <div class="stat-icon" :style="{ background: card.color + '18', color: card.color }">
            <i :class="card.icon"></i>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 收入报表 -->
    <el-card class="report-card" shadow="never">
      <div slot="header" class="report-header">
        <span class="report-title">收入报表</span>
        <div class="report-actions">
          <el-radio-group v-model="quickDate" size="small" @change="handleQuickDate">
            <el-radio-button label="today">今日</el-radio-button>
            <el-radio-button label="week">本周</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
            <el-radio-button label="custom">自定义</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="quickDate === 'custom'"
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
            size="small"
            style="margin-left: 12px"
          />
          <el-button type="primary" size="small" icon="el-icon-search" @click="fetchRevenue" style="margin-left:8px">查询</el-button>
        </div>
      </div>

      <!-- 收入汇总卡片 -->
      <el-row :gutter="16" class="revenue-summary" v-if="revenueList.length > 0">
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">总订单数</div>
            <div class="summary-value">{{ totalOrderCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">总收入</div>
            <div class="summary-value income">¥{{ totalRevenue }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">总退款</div>
            <div class="summary-value refund">¥{{ totalRefund }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="summary-item">
            <div class="summary-label">总售票数</div>
            <div class="summary-value">{{ totalTickets }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 收入表格 -->
      <el-table :data="revenueList" border style="margin-top: 16px" size="small">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="orderCount" label="订单数" width="90" align="center" />
        <el-table-column prop="revenue" label="收入" align="right">
          <template slot-scope="{row}">
            <span style="color:#67C23A;font-weight:bold">¥{{ row.revenue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款" align="right">
          <template slot-scope="{row}">
            <span style="color:#F56C6C">¥{{ row.refundAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ticketCount" label="售票数" width="90" align="center" />
        <el-table-column label="净收入" align="right">
          <template slot-scope="{row}">
            <span style="color:#409EFF;font-weight:bold">¥{{ (parseFloat(row.revenue || 0) - parseFloat(row.refundAmount || 0)).toFixed(2) }}</span>
          </template>
        </el-table-column>
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
      salesData: null,
      statCards: [
        { key: 'totalOrders', label: '总订单数', value: 0, icon: 'el-icon-s-order', color: '#409EFF' },
        { key: 'totalRevenue', label: '总销售额', value: '¥0', icon: 'el-icon-s-finance', color: '#67C23A' },
        { key: 'activeConcerts', label: '活跃演唱会', value: 0, icon: 'el-icon-headset', color: '#E6A23C' },
        { key: 'totalUsers', label: '注册用户', value: 0, icon: 'el-icon-user-solid', color: '#F56C6C' }
      ],
      quickDate: 'month',
      dateRange: [],
      revenueList: []
    }
  },
  computed: {
    totalOrderCount() {
      return this.revenueList.reduce((sum, r) => sum + (r.orderCount || 0), 0)
    },
    totalRevenue() {
      return this.revenueList.reduce((sum, r) => sum + parseFloat(r.revenue || 0), 0).toFixed(2)
    },
    totalRefund() {
      return this.revenueList.reduce((sum, r) => sum + parseFloat(r.refundAmount || 0), 0).toFixed(2)
    },
    totalTickets() {
      return this.revenueList.reduce((sum, r) => sum + (r.ticketCount || 0), 0)
    }
  },
  created() {
    this.fetchSales()
    this.handleQuickDate('month')
  },
  methods: {
    fetchSales() {
      this.loading = true
      getSalesOverview().then(res => {
        const d = res.data
        this.salesData = d
        this.statCards[0].value = d.totalOrders || 0
        this.statCards[1].value = '¥' + (d.totalRevenue || 0)
        this.statCards[2].value = d.activeConcerts || 0
        this.statCards[3].value = d.totalUsers || 0
      }).finally(() => { this.loading = false })
    },
    handleQuickDate(type) {
      const now = new Date()
      let start, end
      end = this.formatDate(now)
      if (type === 'today') {
        start = end
      } else if (type === 'week') {
        const weekAgo = new Date(now)
        weekAgo.setDate(weekAgo.getDate() - 6)
        start = this.formatDate(weekAgo)
      } else if (type === 'month') {
        const monthAgo = new Date(now)
        monthAgo.setMonth(monthAgo.getMonth() - 1)
        start = this.formatDate(monthAgo)
      }
      if (start) {
        this.dateRange = [start, end]
        this.fetchRevenue()
      }
    },
    formatDate(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
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

<style lang="scss" scoped>
.dashboard-page {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border-top: 3px solid;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .stat-icon {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    flex-shrink: 0;
  }

  .stat-content {
    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #303133;
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.report-card {
  border-radius: 12px;

  .report-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .report-title {
      font-size: 16px;
      font-weight: bold;
    }

    .report-actions {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 4px;
    }
  }
}

.revenue-summary {
  margin-top: 16px;

  .summary-item {
    text-align: center;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;

    .summary-label {
      font-size: 13px;
      color: #909399;
      margin-bottom: 6px;
    }

    .summary-value {
      font-size: 20px;
      font-weight: bold;
      color: #303133;

      &.income { color: #67C23A; }
      &.refund { color: #F56C6C; }
    }
  }
}

@media (max-width: 768px) {
  .stat-card { padding: 14px; }
  .stat-card .stat-icon { width: 40px; height: 40px; font-size: 20px; }
  .stat-card .stat-content .stat-value { font-size: 18px; }
  .report-card .report-header { flex-direction: column; align-items: flex-start; }
}
</style>
