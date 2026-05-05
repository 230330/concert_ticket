<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <div slot="header">
        <span>我的取票码</span>
      </div>

      <el-table v-if="ticketList.length > 0" :data="ticketList" border>
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="pickupCode" label="取票码" width="200" />
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template slot-scope="{row}">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="160" />
        <el-table-column label="状态" width="80">
          <template slot-scope="{row}">
            <el-tag :type="row.status === 4 ? 'success' : 'warning'">{{ row.status === 4 ? '已取票' : '待取票' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="暂无取票码" />
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
    fetchData() {
      this.loading = true
      getMyTicketCodes().then(res => {
        this.ticketList = res.data || []
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
