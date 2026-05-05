<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>场次管理</span>
        <el-button type="primary" size="small" style="float:right" @click="handleAdd">新增场次</el-button>
      </div>
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="演唱会ID"><el-input v-model="searchForm.concertId" placeholder="演唱会ID" /></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="concertId" label="演唱会ID" width="80" />
        <el-table-column prop="venueId" label="场馆ID" width="80" />
        <el-table-column prop="showTime" label="演出时间" width="160" />
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
    <el-dialog :title="isEdit?'编辑场次':'新增场次'" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="演唱会ID"><el-input v-model="form.concertId" /></el-form-item>
        <el-form-item label="场馆ID"><el-input v-model="form.venueId" /></el-form-item>
        <el-form-item label="演出时间"><el-date-picker v-model="form.showTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" /></el-form-item>
        <el-form-item label="开售时间"><el-date-picker v-model="form.saleStartTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" /></el-form-item>
        <el-form-item label="停售时间"><el-date-picker v-model="form.saleEndTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { getShowList, addShow, updateShow, deleteShow } from '@/api/admin/show'
export default {
  name: 'AdminShow',
  data() {
    return {
      searchForm: { concertId: '' }, tableData: [], loading: false, currentPage: 1, pageSize: 10, total: 0,
      dialogVisible: false, isEdit: false, editId: null,
      form: { concertId: '', venueId: '', showTime: '', saleStartTime: '', saleEndTime: '' }
    }
  },
  created() { this.fetchData() },
  methods: {
    fetchData() {
      this.loading = true
      getShowList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []; this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleAdd() { this.isEdit = false; this.editId = null; this.form = { concertId: '', venueId: '', showTime: '', saleStartTime: '', saleEndTime: '' }; this.dialogVisible = true },
    handleEdit(row) { this.isEdit = true; this.editId = row.id; this.form = { concertId: row.concertId, venueId: row.venueId, showTime: row.showTime, saleStartTime: row.saleStartTime, saleEndTime: row.saleEndTime }; this.dialogVisible = true },
    handleSubmit() {
      (this.isEdit ? updateShow(this.editId, this.form) : addShow(this.form)).then(() => {
        this.$message.success('操作成功'); this.dialogVisible = false; this.fetchData()
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除？', '提示', { type: 'warning' }).then(() => {
        deleteShow(row.id).then(() => { this.$message.success('删除成功'); this.fetchData() })
      }).catch(() => {})
    }
  }
}
</script>
