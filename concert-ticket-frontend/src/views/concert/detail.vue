<template>
  <div class="app-container" v-loading="loading">
    <el-card v-if="detail">
      <!-- 基本信息 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-image v-if="detail.poster" :src="detail.poster" style="width:100%" fit="cover" />
        </el-col>
        <el-col :span="18">
          <h2>{{ detail.name }}</h2>
          <p style="color:#666;margin:15px 0">{{ detail.description }}</p>
          <el-divider />
          <!-- 艺人信息 -->
          <div v-if="detail.artists && detail.artists.length > 0">
            <h4>参演艺⼈</h4>
            <div v-for="artist in detail.artists" :key="artist.id" style="display:inline-block;margin:5px 10px;text-align:center">
              <el-avatar v-if="artist.avatar" :src="artist.avatar" :size="60" />
              <p>{{ artist.name }}</p>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-divider />

      <!-- 场次列表 -->
      <h3>演出场次</h3>
      <el-table :data="showList" border style="width:100%;margin-top:15px">
        <el-table-column prop="id" label="场次ID" width="80" />
        <el-table-column prop="showTime" label="演出时间" width="180" />
        <el-table-column prop="venueName" label="场馆" width="150" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="row.status === 2 ? 'success' : 'info'">{{ row.status === 2 ? '售票中' : '未开售' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="{row}">
            <el-button type="primary" size="mini" :disabled="row.status !== 2" @click="$router.push(`/concert/seat/${row.id}`)">
              选座购票
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else description="暂无数据" />
  </div>
</template>

<script>
import { getConcertDetail } from '@/api/concert'
import { getShowList } from '@/api/show'

export default {
  name: 'ConcertDetail',
  data() {
    return {
      detail: null,
      showList: [],
      loading: false
    }
  },
  created() {
    this.fetchDetail()
  },
  methods: {
    fetchDetail() {
      const id = this.$route.params.id
      this.loading = true
      getConcertDetail(id).then(res => {
        this.detail = res.data
        if (this.detail) {
          getShowList({ concertId: id }).then(r => {
            this.showList = r.data || []
          })
        }
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
