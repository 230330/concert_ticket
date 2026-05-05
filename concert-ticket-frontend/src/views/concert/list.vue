<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card style="margin-bottom:20px">
      <el-form :inline="true" :model="searchForm" size="small">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="演唱会名称" clearable />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="searchForm.city" placeholder="城市" clearable />
        </el-form-item>
        <el-form-item label="艺人">
          <el-input v-model="searchForm.artistName" placeholder="艺人名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 标签切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane label="热门演唱会" name="hot" />
      <el-tab-pane label="即将开始" name="upcoming" />
      <el-tab-pane label="搜索结果" name="search" v-if="isSearch" />
    </el-tabs>

    <!-- 列表 -->
    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="演唱会名称" min-width="180" />
      <el-table-column prop="poster" label="海报" width="100">
        <template slot-scope="{row}">
          <el-image v-if="row.poster" :src="row.poster" style="width:60px;height:60px" fit="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template slot-scope="{row}">
          <el-button type="primary" size="mini" @click="$router.push(`/concert/detail/${row.id}`)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      style="margin-top:20px;text-align:right"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page.sync="currentPage"
      @current-change="fetchData"
    />
  </div>
</template>

<script>
import { getHotConcerts, getUpcomingConcerts, searchConcerts } from '@/api/concert'

export default {
  name: 'ConcertList',
  data() {
    return {
      searchForm: { keyword: '', city: '', artistName: '' },
      activeTab: 'hot',
      isSearch: false,
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      let apiCall
      const params = { page: this.currentPage, size: this.pageSize }

      if (this.activeTab === 'hot') {
        apiCall = getHotConcerts(params)
      } else if (this.activeTab === 'upcoming') {
        apiCall = getUpcomingConcerts(params)
      } else {
        apiCall = searchConcerts({ ...this.searchForm, ...params })
      }

      apiCall.then(res => {
        this.tableData = res.data.records || []
        this.total = res.data.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    handleTabChange(tab) {
      this.currentPage = 1
      this.isSearch = tab.name === 'search'
      this.fetchData()
    },
    handleSearch() {
      this.isSearch = true
      this.activeTab = 'search'
      this.currentPage = 1
      this.fetchData()
    },
    handleReset() {
      this.searchForm = { keyword: '', city: '', artistName: '' }
      this.isSearch = false
      this.activeTab = 'hot'
      this.currentPage = 1
      this.fetchData()
    }
  }
}
</script>
