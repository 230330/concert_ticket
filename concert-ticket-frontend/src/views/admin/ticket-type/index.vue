<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>票种管理</span>
        <el-button type="primary" size="small" style="float:right" @click="handleAdd">新增票种</el-button>
      </div>
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="场次ID"><el-input v-model="searchForm.showId" placeholder="场次ID" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="showId" label="场次ID" width="70" />
        <el-table-column prop="name" label="票种名称" min-width="100" />
        <el-table-column prop="price" label="价格" width="80" />
        <el-table-column label="库存" width="130">
          <template slot-scope="{row}">{{ row.availableStock }} / {{ row.totalStock }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="small" @click="handleStock(row)">调库存</el-button>
            <el-button type="text" size="small" style="color:#F56C6C" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total>0" style="margin-top:20px;text-align:right" background layout="total,prev,pager,next"
        :total="total" :page-size="pageSize" :current-page.sync="currentPage" @current-change="fetchData" />
    </el-card>

    <el-dialog :title="isEdit?'编辑票种':'新增票种'" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="场次ID"><el-input v-model="form.showId" /></el-form-item>
        <el-form-item label="区域ID"><el-input v-model="form.areaId" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="总库存"><el-input-number v-model="form.totalStock" :min="1" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></div>
    </el-dialog>

    <el-dialog title="库存调整" :visible.sync="stockVisible" width="400px">
      <el-form label-width="80px">
        <el-form-item label="当前库存">{{ stockForm.availableStock }} / {{ stockForm.totalStock }}</el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="stockForm.adjustQuantity" :min="-1000" :max="1000" />
        </el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="stockVisible=false">取消</el-button><el-button type="primary" @click="handleStockSubmit">确定</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { getTicketTypeList, addTicketType, updateTicketType, deleteTicketType, adjustStock } from '@/api/admin/ticketType'
export default {
  name: 'AdminTicketType',
  data() {
    return {
      searchForm: { showId: '' }, tableData: [], loading: false, currentPage: 1, pageSize: 10, total: 0,
      dialogVisible: false, isEdit: false, editId: null,
      form: { showId: '', areaId: '', name: '', price: 0, totalStock: 100 },
      stockVisible: false,
      stockForm: { ticketTypeId: null, availableStock: 0, totalStock: 0, adjustQuantity: 0 }
    }
  },
  created() { this.fetchData() },
  methods: {
    fetchData() {
      this.loading = true
      getTicketTypeList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []; this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleAdd() { this.isEdit = false; this.editId = null; this.form = { showId: '', areaId: '', name: '', price: 0, totalStock: 100 }; this.dialogVisible = true },
    handleEdit(row) { this.isEdit = true; this.editId = row.id; this.form = { showId: row.showId, areaId: row.areaId, name: row.name, price: row.price, totalStock: row.totalStock }; this.dialogVisible = true },
    handleSubmit() {
      (this.isEdit ? updateTicketType(this.editId, this.form) : addTicketType(this.form)).then(() => {
        this.$message.success('操作成功'); this.dialogVisible = false; this.fetchData()
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除？', '提示', { type: 'warning' }).then(() => {
        deleteTicketType(row.id).then(() => { this.$message.success('删除成功'); this.fetchData() })
      }).catch(() => {})
    },
    handleStock(row) {
      this.stockForm = { ticketTypeId: row.id, availableStock: row.availableStock, totalStock: row.totalStock, adjustQuantity: 0 }
      this.stockVisible = true
    },
    handleStockSubmit() {
      adjustStock({ ticketTypeId: this.stockForm.ticketTypeId, adjustQuantity: this.stockForm.adjustQuantity }).then(() => {
        this.$message.success('库存调整成功'); this.stockVisible = false; this.fetchData()
      })
    }
  }
}
</script>
