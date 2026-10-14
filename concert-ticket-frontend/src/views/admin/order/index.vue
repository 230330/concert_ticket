<template>
  <div class="admin-order-page">
    <el-card shadow="never" class="page-card">
      <template #header>
          <span class="page-title">订单管理</span>
      </template>

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
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="fetchData">搜索</el-button>
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
            <el-button type="text" size="small" icon="View" @click="viewDetail(row)">详情</el-button>
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
        v-model:current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script>
import { getOrderList, adminRefund, adminCancel } from '@/api/admin/order'
import { ORDER_STATUS_MAP, resolveStatusLabel, resolveStatusTagType, toStatusOptions } from '@/constants/status'

export default {
  name: 'AdminOrder',
  data() {
    return {
      searchForm: { orderNo: '', userId: '', status: '' },
      statusOptions: toStatusOptions(ORDER_STATUS_MAP),
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  created() { this.fetchData() },
  methods: {
    statusText(s) { return resolveStatusLabel(ORDER_STATUS_MAP, s) },
    statusTag(s) { return resolveStatusTagType(ORDER_STATUS_MAP, s) },
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
        adminCancel(row.id).then(() => {
          this.$message.success('已取消')
          this.fetchData()
        })
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
