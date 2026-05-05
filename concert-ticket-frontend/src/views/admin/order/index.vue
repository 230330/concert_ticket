<template>
  <div class="app-container">
    <el-card>
      <div slot="header"><span>订单管理</span></div>
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="订单编号"><el-input v-model="searchForm.orderNo" placeholder="订单编号" clearable /></el-form-item>
        <el-form-item label="用户ID"><el-input v-model="searchForm.userId" placeholder="用户ID" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:120px">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已退款" :value="3" />
            <el-option label="已完成" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">搜索</el-button></el-form-item>
      </el-form>

      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="orderNo" label="订单编号" width="160" show-overflow-tooltip />
        <el-table-column prop="userPhone" label="用户手机号" width="120" />
        <el-table-column prop="concertName" label="演唱会" min-width="120" />
        <el-table-column prop="totalAmount" label="金额" width="80">
          <template slot-scope="{row}">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template slot-scope="{row}">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="$router.push(`/order/detail/${row.id}`)">详情</el-button>
            <el-button v-if="row.status===0" type="text" size="small" style="color:#F56C6C" @click="handleAdminCancel(row)">取消</el-button>
            <el-button v-if="row.status===1||row.status===4" type="text" size="small" style="color:#E6A23C" @click="handleAdminRefund(row)">退款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total>0" style="margin-top:20px;text-align:right" background layout="total,prev,pager,next"
        :total="total" :page-size="pageSize" :current-page.sync="currentPage" @current-change="fetchData" />
    </el-card>
  </div>
</template>

<script>
import { getOrderList, adminCancel, adminRefund } from '@/api/admin/order'

const S_MAP = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' }
const T_MAP = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: '' }

export default {
  name: 'AdminOrder',
  data() {
    return {
      searchForm: { orderNo: '', userId: '', status: '' },
      tableData: [], loading: false, currentPage: 1, pageSize: 10, total: 0
    }
  },
  created() { this.fetchData() },
  methods: {
    statusText(s) { return S_MAP[s] || '未知' },
    statusTag(s) { return T_MAP[s] || 'info' },
    fetchData() {
      this.loading = true
      getOrderList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []; this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleAdminCancel(row) {
      this.$confirm('确认取消该订单？', '提示', { type: 'warning' }).then(() => {
        adminCancel(row.id).then(() => { this.$message.success('已取消'); this.fetchData() })
      }).catch(() => {})
    },
    handleAdminRefund(row) {
      this.$confirm('确认退款该订单？', '提示', { type: 'warning' }).then(() => {
        adminRefund(row.id).then(() => { this.$message.success('已退款'); this.fetchData() })
      }).catch(() => {})
    }
  }
}
</script>
