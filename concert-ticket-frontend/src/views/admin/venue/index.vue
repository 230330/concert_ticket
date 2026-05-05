<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>场馆管理</span>
        <el-button type="primary" size="small" style="float:right" @click="handleAdd">新增场馆</el-button>
      </div>
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="名称"><el-input v-model="searchForm.name" placeholder="场馆名称" clearable /></el-form-item>
        <el-form-item label="城市"><el-input v-model="searchForm.city" placeholder="城市" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="capacity" label="容量" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="small" style="color:#F56C6C" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total>0" style="margin-top:20px;text-align:right" background layout="total,prev,pager,next"
        :total="total" :page-size="pageSize" :current-page.sync="currentPage" @current-change="fetchData" />
    </el-card>
    <el-dialog :title="isEdit?'编辑场馆':'新增场馆'" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="1" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { getVenueList, addVenue, updateVenue, deleteVenue } from '@/api/admin/venue'
export default {
  name: 'AdminVenue',
  data() {
    return {
      searchForm: { name: '', city: '' }, tableData: [], loading: false, currentPage: 1, pageSize: 10, total: 0,
      dialogVisible: false, isEdit: false, editId: null, form: { name: '', city: '', address: '', capacity: 100 }
    }
  },
  created() { this.fetchData() },
  methods: {
    fetchData() {
      this.loading = true
      getVenueList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []; this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleAdd() { this.isEdit = false; this.editId = null; this.form = { name: '', city: '', address: '', capacity: 100 }; this.dialogVisible = true },
    handleEdit(row) { this.isEdit = true; this.editId = row.id; this.form = { name: row.name, city: row.city, address: row.address, capacity: row.capacity }; this.dialogVisible = true },
    handleSubmit() {
      (this.isEdit ? updateVenue(this.editId, this.form) : addVenue(this.form)).then(() => {
        this.$message.success('操作成功'); this.dialogVisible = false; this.fetchData()
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除？', '提示', { type: 'warning' }).then(() => {
        deleteVenue(row.id).then(() => { this.$message.success('删除成功'); this.fetchData() })
      }).catch(() => {})
    }
  }
}
</script>
