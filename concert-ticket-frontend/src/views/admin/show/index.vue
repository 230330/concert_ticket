<template>
  <div class="app-container">
    <el-card>
      <div slot="header">
        <span>场次管理</span>
        <el-button type="primary" size="small" style="float:right" @click="handleAdd">新增场次</el-button>
      </div>
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="演唱会ID"><el-input v-model="searchForm.concertId" placeholder="演唱会ID" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="fetchData">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="concertId" label="演唱会ID" width="80" />
        <el-table-column prop="venueId" label="场馆ID" width="80" />
        <el-table-column prop="showTime" label="演出时间" width="160" />
        <el-table-column prop="saleStartTime" label="开售时间" width="160" />
        <el-table-column prop="saleEndTime" label="停售时间" width="160" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{row}">
            <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="text" size="small" @click="handleChangeStatus(row)">变更状态</el-button>
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
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></div>
    </el-dialog>

    <!-- 状态变更对话框 -->
    <el-dialog title="变更场次状态" :visible.sync="statusDialogVisible" width="420px">
      <el-form label-width="80px">
        <el-form-item label="当前状态">
          <el-tag :type="statusTagType(currentShowStatus)">{{ statusLabel(currentShowStatus) }}</el-tag>
        </el-form-item>
        <el-form-item label="目标状态">
          <el-select v-model="targetStatus" placeholder="请选择目标状态">
            <el-option v-for="item in availableStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="targetStatus === '' || targetStatus === null" @click="handleSubmitStatus">确认变更</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getShowList, addShow, updateShow, deleteShow, updateShowStatus } from '@/api/admin/show'

// 场次状态枚举映射（与后端 ShowStatus 一致）
const STATUS_MAP = {
  0: { label: '未开售', tagType: 'info' },
  1: { label: '售票中', tagType: 'warning' },
  2: { label: '已售罄', tagType: 'danger' },
  3: { label: '已结束', tagType: 'success' },
  4: { label: '已取消', tagType: 'info' }
}

// 状态转换规则：当前状态 → 允许的目标状态列表
const STATUS_TRANSITIONS = {
  0: [1, 4], // 未开售 → 售票中、已取消
  1: [2, 4], // 售票中 → 已售罄、已取消
  2: [3, 4], // 已售罄 → 已结束、已取消
  3: [],     // 已结束 → 终态
  4: []      // 已取消 → 终态
}

export default {
  name: 'AdminShow',
  data() {
    return {
      searchForm: { concertId: '', status: null },
      tableData: [], loading: false, currentPage: 1, pageSize: 10, total: 0,
      dialogVisible: false, isEdit: false, editId: null,
      form: { concertId: '', venueId: '', showTime: '', saleStartTime: '', saleEndTime: '', status: 0 },
      // 状态变更相关
      statusDialogVisible: false,
      statusEditId: null,
      currentShowStatus: null,
      targetStatus: null,
      statusOptions: Object.entries(STATUS_MAP).map(([value, { label }]) => ({ value: Number(value), label }))
    }
  },
  computed: {
    availableStatusOptions() {
      if (this.currentShowStatus === null) return []
      const allowed = STATUS_TRANSITIONS[this.currentShowStatus] || []
      return allowed.map(value => ({ value, label: STATUS_MAP[value].label }))
    }
  },
  created() { this.fetchData() },
  methods: {
    statusLabel(status) {
      return STATUS_MAP[status] ? STATUS_MAP[status].label : '未知'
    },
    statusTagType(status) {
      return STATUS_MAP[status] ? STATUS_MAP[status].tagType : 'info'
    },
    fetchData() {
      this.loading = true
      getShowList({ page: this.currentPage, size: this.pageSize, ...this.searchForm }).then(res => {
        this.tableData = res.data.records || []; this.total = res.data.total || 0
      }).finally(() => { this.loading = false })
    },
    handleAdd() {
      this.isEdit = false; this.editId = null
      this.form = { concertId: '', venueId: '', showTime: '', saleStartTime: '', saleEndTime: '', status: 0 }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.isEdit = true; this.editId = row.id
      this.form = { concertId: row.concertId, venueId: row.venueId, showTime: row.showTime, saleStartTime: row.saleStartTime, saleEndTime: row.saleEndTime, status: row.status }
      this.dialogVisible = true
    },
    handleSubmit() {
      (this.isEdit ? updateShow(this.editId, this.form) : addShow(this.form)).then(() => {
        this.$message.success('操作成功'); this.dialogVisible = false; this.fetchData()
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除？', '提示', { type: 'warning' }).then(() => {
        deleteShow(row.id).then(() => { this.$message.success('删除成功'); this.fetchData() })
      }).catch(() => {})
    },
    handleChangeStatus(row) {
      this.statusEditId = row.id
      this.currentShowStatus = row.status
      this.targetStatus = null
      this.statusDialogVisible = true
    },
    handleSubmitStatus() {
      if (this.targetStatus === null || this.targetStatus === '') return
      const targetLabel = this.statusLabel(this.targetStatus)
      this.$confirm(`确认将场次状态变更为"${targetLabel}"？`, '状态变更确认', { type: 'warning' }).then(() => {
        updateShowStatus(this.statusEditId, { status: this.targetStatus }).then(() => {
          this.$message.success('状态变更成功')
          this.statusDialogVisible = false
          this.fetchData()
        })
      }).catch(() => {})
    }
  }
}
</script>
