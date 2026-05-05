<template>
  <div class="app-container">
    <el-card>
      <div slot="header"><span>用户管理</span></div>
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="手机号"><el-input v-model="searchForm.phone" placeholder="手机号" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">搜索</el-button></el-form-item>
      </el-form>

      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="orderCount" label="订单数" width="80" />
        <el-table-column label="状态" width="80">
          <template slot-scope="{row}">
            <el-tag :type="row.status===0?'danger':'success'">{{ row.status===0?'已封禁':'正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{row}">
            <el-button v-if="row.status!==0" type="text" size="small" style="color:#F56C6C" @click="handleBan(row)">封禁</el-button>
            <el-button v-if="row.status===0" type="text" size="small" style="color:#67C23A" @click="handleUnban(row)">解封</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total>0" style="margin-top:20px;text-align:right" background layout="total,prev,pager,next"
        :total="total" :page-size="pageSize" :current-page.sync="currentPage" @current-change="fetchData" />
    </el-card>
  </div>
</template>

<script>
import { getUserList, banUser, unbanUser } from '@/api/admin/user'

export default {
  name: 'AdminUser',
  data() {
    return {
      searchForm: { phone: '' },
      tableData: [], loading: false, currentPage: 1, pageSize: 10, total: 0
    }
  },
  created() { this.fetchData() },
  methods: {
    fetchData() {
      this.loading = true
      getUserList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []; this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleBan(row) {
      this.$confirm('确认封禁该用户？', '提示', { type: 'warning' }).then(() => {
        banUser({ userId: row.id, status: 0 }).then(() => { this.$message.success('已封禁'); this.fetchData() })
      }).catch(() => {})
    },
    handleUnban(row) {
      this.$confirm('确认解封该用户？', '提示', { type: 'warning' }).then(() => {
        unbanUser({ userId: row.id, status: 1 }).then(() => { this.$message.success('已解封'); this.fetchData() })
      }).catch(() => {})
    }
  }
}
</script>
