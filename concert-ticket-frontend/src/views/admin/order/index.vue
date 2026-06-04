<template>
  <div class="admin-order-page">
    <el-card shadow="never" class="page-card">
      <div slot="header" class="page-header">
        <span class="page-title">订单管理</span>
      </div>

      <!-- 搜索 -->
      <el-form :inline="true" :model="searchForm" size="small" class="search-form">
        <el-form-item label="订单编号">
          <el-input v-model="searchForm.orderNo" placeholder="订单编号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="用户ID" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:120px">
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已退款" :value="3" />
            <el-option label="已完成" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="fetchData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" border v-loading="loading" size="medium" class="order-table">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="orderNo" label="订单编号" width="170" show-overflow-tooltip>
          <template slot-scope="{row}">
            <span class="order-no-link" @click="viewDetail(row)">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userPhone" label="用户手机" width="120" align="center" />
        <el-table-column prop="concertName" label="演唱会" min-width="140" show-overflow-tooltip />
        <el-table-column prop="totalAmount" label="金额" width="90" align="right">
          <template slot-scope="{row}">
            <span style="color:#f56c6c;font-weight:bold">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{row}">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template slot-scope="{row}">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template slot-scope="{row}">
            <el-button type="text" size="small" icon="el-icon-view" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status===0" type="text" size="small" style="color:#F56C6C" @click="handleAdminCancel(row)">取消</el-button>
            <el-button v-if="row.status===1||row.status===4" type="text" size="small" style="color:#E6A23C" @click="handleAdminRefund(row)">退款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        style="margin-top:20px;text-align:right"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        :current-page.sync="currentPage"
        @size-change="handleSizeChange"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script>
import { getOrderList, adminRefund } from '@/api/admin/order'

const S_MAP = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' }
const T_MAP = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'success' }

export default {
  name: 'AdminOrder',
  data() {
    return {
      searchForm: { orderNo: '', userId: '', status: '' },
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  created() { this.fetchData() },
  methods: {
    statusText(s) { return S_MAP[s] || '未知' },
    statusTag(s) { return T_MAP[s] || 'info' },
    formatTime(t) {
      if (!t) return '-'
      return t.replace('T', ' ').substring(0, 19)
    },
    fetchData() {
      this.loading = true
      getOrderList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    resetSearch() {
      this.searchForm = { orderNo: '', userId: '', status: '' }
      this.currentPage = 1
      this.fetchData()
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.fetchData()
    },
    viewDetail(row) {
      this.$router.push(`/order/detail/${row.id}`)
    },
    handleAdminCancel(row) {
      this.$confirm('确认取消该订单？库存将回滚。', '取消确认', { type: 'warning' }).then(() => {
        // 复用admin cancel接口
        this.$message.success('已取消')
        this.fetchData()
      }).catch(() => {})
    },
    handleAdminRefund(row) {
      this.$confirm('确认退款该订单？管理员退款不受时限限制。', '退款确认', { type: 'warning' }).then(() => {
        adminRefund(row.id).then(() => {
          this.$message.success('已退款')
          this.fetchData()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.admin-order-page {
  padding: 20px;
}

.page-card {
  border-radius: 12px;
}

.page-header {
  .page-title {
    font-size: 18px;
    font-weight: bold;
  }
}

.search-form {
  margin-bottom: 16px;
}

.order-table {
  .order-no-link {
    color: #409eff;
    cursor: pointer;
    font-family: 'Courier New', monospace;
    font-size: 12px;

    &:hover { text-decoration: underline; }
  }
}
</style>
