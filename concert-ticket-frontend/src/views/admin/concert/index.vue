<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>演唱会管理</span>
        <el-button type="primary" size="small" style="float:right" @click="handleAdd">新增演唱会</el-button>
      </div>
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="名称">
          <el-input v-model="searchForm.name" placeholder="演唱会名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="演唱会名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
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

    <el-dialog :title="isEdit ? '编辑演唱会' : '新增演唱会'" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="海报URL"><el-input v-model="form.poster" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getConcertList, addConcert, updateConcert, deleteConcert } from '@/api/admin/concert'

export default {
  name: 'AdminConcert',
  data() {
    return {
      searchForm: { name: '' },
      tableData: [], loading: false, currentPage: 1, pageSize: 10, total: 0,
      dialogVisible: false, isEdit: false, editId: null,
      form: { name: '', poster: '', description: '' }
    }
  },
  created() { this.fetchData() },
  methods: {
    fetchData() {
      this.loading = true
      getConcertList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleAdd() { this.isEdit = false; this.editId = null; this.form = { name: '', poster: '', description: '' }; this.dialogVisible = true },
    handleEdit(row) { this.isEdit = true; this.editId = row.id; this.form = { name: row.name, poster: row.poster, description: row.description }; this.dialogVisible = true },
    handleSubmit() {
      const api = this.isEdit ? updateConcert(this.editId, this.form) : addConcert(this.form)
      api.then(() => { this.$message.success('操作成功'); this.dialogVisible = false; this.fetchData() })
    },
    handleDelete(row) {
      this.$confirm('确认删除？', '提示', { type: 'warning' }).then(() => {
        deleteConcert(row.id).then(() => { this.$message.success('删除成功'); this.fetchData() })
      }).catch(() => {})
    }
  }
}
</script>
