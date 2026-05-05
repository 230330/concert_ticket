<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <div slot="header">
        <span>选座购票</span>
        <span style="float:right;color:#999;font-size:13px">场次ID：{{ showId }}</span>
      </div>

      <!-- 座位图简化为区域列表 -->
      <div v-if="seatMap">
        <h4>票种区域</h4>
        <el-row :gutter="20" style="margin-top:15px">
          <el-col :span="6" v-for="ticket in seatMap.ticketTypes" :key="ticket.id">
            <el-card shadow="hover" :body-style="{ padding: '15px' }">
              <h4>{{ ticket.name }}</h4>
              <p>价格：<strong style="color:#F56C6C">¥{{ ticket.price }}</strong></p>
              <p>剩余：{{ ticket.availableStock }} / {{ ticket.totalStock }}</p>
              <el-button type="primary" size="small" style="margin-top:10px" :disabled="ticket.availableStock <= 0" @click="handleBuy(ticket)">
                {{ ticket.availableStock > 0 ? '立即购买' : '已售罄' }}
              </el-button>
            </el-card>
          </el-col>
        </el-row>

        <!-- 区域座位 -->
        <div v-if="seatMap.areas && seatMap.areas.length > 0" style="margin-top:30px">
          <h4>区域分布</h4>
          <div v-for="area in seatMap.areas" :key="area.id" style="margin:10px 0">
            <el-tag>{{ area.name }}</el-tag>
            <span style="margin-left:10px;color:#666">{{ area.seats ? area.seats.length : 0 }} 个座位</span>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无座位信息" />
    </el-card>

    <!-- 购买弹窗 -->
    <el-dialog title="确认购买" :visible.sync="dialogVisible" width="400px">
      <el-form :model="buyForm" label-width="80px">
        <el-form-item label="票种">{{ buyForm.ticketTypeName }}</el-form-item>
        <el-form-item label="单价">¥{{ buyForm.price }}</el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="buyForm.quantity" :min="1" :max="4" />
        </el-form-item>
        <el-form-item label="合计">
          <strong style="color:#F56C6C;font-size:18px">¥{{ buyForm.price * buyForm.quantity }}</strong>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBuy">确认下单</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getSeatMap } from '@/api/show'
import { createOrder } from '@/api/order'

export default {
  name: 'SeatSelect',
  data() {
    return {
      showId: null,
      seatMap: null,
      loading: false,
      dialogVisible: false,
      buyForm: {
        ticketTypeId: null,
        ticketTypeName: '',
        price: 0,
        quantity: 1
      }
    }
  },
  created() {
    this.showId = this.$route.params.showId
    this.fetchSeatMap()
  },
  methods: {
    fetchSeatMap() {
      this.loading = true
      getSeatMap(this.showId).then(res => {
        this.seatMap = res.data
      }).finally(() => {
        this.loading = false
      })
    },
    handleBuy(ticket) {
      this.buyForm.ticketTypeId = ticket.id
      this.buyForm.ticketTypeName = ticket.name
      this.buyForm.price = ticket.price
      this.buyForm.quantity = 1
      this.dialogVisible = true
    },
    submitBuy() {
      createOrder({
        showId: parseInt(this.showId),
        ticketTypeId: this.buyForm.ticketTypeId,
        quantity: this.buyForm.quantity
      }).then(res => {
        this.$message.success('下单成功！')
        this.dialogVisible = false
        this.$router.push(`/order/detail/${res.data.id}`)
      }).catch(() => {})
    }
  }
}
</script>
