<template>
  <div class="concert-list-page">
    <!-- 搜索栏 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" size="medium">
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="搜索演唱会名称" clearable prefix-icon="el-icon-search" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.city" placeholder="城市" clearable style="width:120px" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchForm.artistName" placeholder="艺人" clearable style="width:120px" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.sort" placeholder="排序" style="width:110px" clearable>
            <el-option label="默认排序" value="default" />
            <el-option label="按时间" value="time" />
            <el-option label="按名称" value="name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 标签切换 -->
    <div class="tab-bar">
      <el-radio-group v-model="activeTab" @change="handleTabChange" size="medium">
        <el-radio-button label="hot">热门演唱会</el-radio-button>
        <el-radio-button label="upcoming">即将开始</el-radio-button>
        <el-radio-button label="search" v-if="isSearch">搜索结果</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 卡片列表 -->
    <div class="concert-grid" v-loading="loading">
      <div v-for="item in tableData" :key="item.id" class="concert-card" @click="$router.push(`/concert/detail/${item.id}`)">
        <div class="card-poster">
          <el-image v-if="item.poster" :src="item.poster" fit="cover" class="poster-img" />
          <div v-else class="poster-placeholder">
            <i class="el-icon-headset"></i>
          </div>
          <div class="card-price" v-if="item.minPrice">{{ item.minPrice }}起</div>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ item.name }}</h3>
          <div class="card-artists" v-if="item.artistNames && item.artistNames.length > 0">
            <el-tag size="mini" type="info" v-for="name in item.artistNames.slice(0,3)" :key="name" style="margin-right:4px">{{ name }}</el-tag>
          </div>
          <div class="card-meta">
            <span v-if="item.city"><i class="el-icon-location"></i> {{ item.city }}</span>
            <span v-if="item.venueName"><i class="el-icon-office-building"></i> {{ item.venueName }}</span>
          </div>
          <div class="card-meta" v-if="item.nearestShowTime">
            <span><i class="el-icon-time"></i> {{ formatTime(item.nearestShowTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && tableData.length === 0" description="暂无演唱会信息">
      <el-button type="primary" @click="handleReset">查看热门推荐</el-button>
    </el-empty>

    <!-- 分页 -->
    <div class="pagination-bar" v-if="total > 0">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="currentPage"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script>
import { getHotConcerts, getUpcomingConcerts, searchConcerts } from '@/api/concert'

export default {
  name: 'ConcertList',
  data() {
    return {
      searchForm: { keyword: '', city: '', artistName: '', sort: 'default' },
      activeTab: 'hot',
      isSearch: false,
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 12,
      total: 0
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    formatTime(t) {
      if (!t) return ''
      return t.replace('T', ' ').substring(0, 16)
    },
    fetchData() {
      this.loading = true
      let apiCall
      const params = { page: this.currentPage, size: this.pageSize }

      if (this.activeTab === 'hot') {
        params.sort = this.searchForm.sort || 'default'
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
    handleTabChange() {
      this.currentPage = 1
      this.isSearch = this.activeTab === 'search'
      this.fetchData()
    },
    handleSearch() {
      this.isSearch = true
      this.activeTab = 'search'
      this.currentPage = 1
      this.fetchData()
    },
    handleReset() {
      this.searchForm = { keyword: '', city: '', artistName: '', sort: 'default' }
      this.isSearch = false
      this.activeTab = 'hot'
      this.currentPage = 1
      this.fetchData()
    }
  }
}
</script>

<style lang="scss" scoped>
.concert-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.tab-bar {
  margin-bottom: 20px;
}

.concert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.concert-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
}

.card-poster {
  position: relative;
  height: 180px;
  background: #f5f7fa;

  .poster-img {
    width: 100%;
    height: 100%;
  }

  .poster-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

    i {
      font-size: 48px;
      color: rgba(255, 255, 255, 0.6);
    }
  }

  .card-price {
    position: absolute;
    bottom: 10px;
    right: 10px;
    background: rgba(245, 108, 108, 0.9);
    color: #fff;
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: bold;
  }
}

.card-body {
  padding: 14px 16px;

  .card-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
    margin: 0 0 8px 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-artists {
    margin-bottom: 8px;
  }

  .card-meta {
    display: flex;
    gap: 12px;
    font-size: 13px;
    color: #909399;
    margin-top: 4px;

    i { margin-right: 2px; }

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.pagination-bar {
  margin-top: 30px;
  text-align: center;
}

@media (max-width: 768px) {
  .concert-list-page { padding: 10px; }
  .concert-grid { grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
  .card-poster { height: 150px; }
}
</style>
