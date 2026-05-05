<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>热门演唱会</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="$router.push('/concert/list')">查看更多</el-button>
          </div>
          <div v-if="hotList.length === 0" class="text-muted">暂无数据</div>
          <div v-for="item in hotList" :key="item.id" class="concert-item" @click="$router.push(`/concert/detail/${item.id}`)">
            <span>{{ item.name }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>即将开始</span>
            <el-button style="float: right; padding: 3px 0" type="text" @click="$router.push('/concert/list')">查看更多</el-button>
          </div>
          <div v-if="upcomingList.length === 0" class="text-muted">暂无数据</div>
          <div v-for="item in upcomingList" :key="item.id" class="concert-item" @click="$router.push(`/concert/detail/${item.id}`)">
            <span>{{ item.name }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>快捷入口</span>
          </div>
          <el-button type="primary" style="width:100%;margin-bottom:10px" @click="$router.push('/order/my')">我的订单</el-button>
          <el-button type="success" style="width:100%;margin-bottom:10px" @click="$router.push('/ticket/my-codes')">我的取票码</el-button>
          <el-button type="info" style="width:100%" @click="$router.push('/profile/index')">个人中心</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getHotConcerts, getUpcomingConcerts } from '@/api/concert'

export default {
  name: 'Dashboard',
  data() {
    return {
      hotList: [],
      upcomingList: []
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      getHotConcerts({ page: 1, size: 5 }).then(res => {
        this.hotList = res.data.records || []
      }).catch(() => {})
      getUpcomingConcerts({ page: 1, size: 5 }).then(res => {
        this.upcomingList = res.data.records || []
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  margin: 30px;
}
.concert-item {
  padding: 10px 0;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  &:hover {
    color: #409EFF;
  }
}
.text-muted {
  color: #999;
  text-align: center;
  padding: 20px 0;
}
</style>
