<template>
  <div class="seat-page" v-loading="loading">
    <!-- 头部信息 -->
    <div class="seat-header">
      <el-page-header @back="$router.back()" content="选座购票" />
      <div class="show-info" v-if="seatMap">
        <span class="venue-name">{{ seatMap.venueName }}</span>
      </div>
    </div>

    <!-- 区域选择标签 -->
    <div class="area-tabs" v-if="seatMap && seatMap.areas && seatMap.areas.length > 0">
      <div
        v-for="area in seatMap.areas"
        :key="area.id"
        :class="['area-tab', { active: selectedArea && selectedArea.id === area.id, soldout: area.capacity === 0 }]"
        @click="selectArea(area)"
      >
        <span class="area-name">{{ area.name }}</span>
        <span class="area-price">¥{{ area.price }}</span>
        <span class="area-stock">{{ getAreaAvailable(area) }}张可售</span>
      </div>
    </div>

    <!-- 座位图 -->
    <div class="seat-stage" v-if="selectedArea">
      <div class="stage-label">舞 台</div>
      <div class="seat-grid-wrapper">
        <div class="seat-grid">
          <div v-for="row in selectedArea.rows" :key="row.rowCode" class="seat-row">
            <span class="row-label">{{ row.rowCode }}</span>
            <div
              v-for="seat in row.seats"
              :key="seat.id"
              :class="['seat', {
                sold: seat.sold,
                selected: isSelected(seat.id),
                available: !seat.sold
              }]"
              @click="toggleSeat(seat)"
              :title="seat.sold ? '已售' : seat.seatNo + ' ¥' + selectedArea.price"
            >
              <span class="seat-number">{{ seat.colNum }}</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 图例 -->
      <div class="seat-legend">
        <span class="legend-item"><i class="seat-icon available"></i>可选</span>
        <span class="legend-item"><i class="seat-icon selected"></i>已选</span>
        <span class="legend-item"><i class="seat-icon sold"></i>已售</span>
      </div>
    </div>

    <el-empty v-if="!seatMap && !loading" description="暂无座位信息" />

    <!-- 底部确认面板 -->
    <div class="seat-footer" v-if="selectedSeats.length > 0">
      <div class="selected-info">
        <div class="selected-seats">
          <el-tag
            v-for="s in selectedSeats"
            :key="s.id"
            closable
            size="small"
            type="success"
            @close="removeSeat(s)"
            style="margin: 2px 4px"
          >
            {{ s.seatNo }}
          </el-tag>
        </div>
        <div class="price-summary">
          <span class="count">已选 <strong>{{ selectedSeats.length }}</strong> 张</span>
          <span class="total-price">合计：<strong>¥{{ totalPrice }}</strong></span>
        </div>
      </div>
      <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="submitBuy">
        立即下单 ¥{{ totalPrice }}
      </el-button>
    </div>

    <!-- 购买确认弹窗 -->
    <el-dialog title="确认订单" :visible.sync="dialogVisible" width="420px" center>
      <div class="confirm-order">
        <div class="confirm-item">
          <span class="label">区域</span>
          <span>{{ selectedArea ? selectedArea.name : '' }}</span>
        </div>
        <div class="confirm-item">
          <span class="label">票价</span>
          <span class="price">¥{{ selectedArea ? selectedArea.price : 0 }}/张</span>
        </div>
        <div class="confirm-item">
          <span class="label">座位</span>
          <span>{{ selectedSeats.map(s => s.seatNo).join('、') }}</span>
        </div>
        <div class="confirm-item total">
          <span class="label">合计</span>
          <span class="price total-price">¥{{ totalPrice }}</span>
        </div>
      </div>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmOrder">确认支付</el-button>
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
      selectedArea: null,
      selectedSeats: [],
      loading: false,
      submitting: false,
      dialogVisible: false,
      maxSelect: 4
    }
  },
  computed: {
    totalPrice() {
      if (!this.selectedArea || this.selectedSeats.length === 0) return 0
      return (this.selectedArea.price * this.selectedSeats.length).toFixed(2)
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
        // 默认选中第一个有座位的区域
        if (this.seatMap && this.seatMap.areas && this.seatMap.areas.length > 0) {
          const availableArea = this.seatMap.areas.find(a => this.getAreaAvailable(a) > 0)
          if (availableArea) {
            this.selectArea(availableArea)
          }
        }
      }).finally(() => {
        this.loading = false
      })
    },
    selectArea(area) {
      this.selectedArea = area
      // 切换区域时清除已选座位
      this.selectedSeats = []
    },
    getAreaAvailable(area) {
      if (!area.rows) return 0
      let count = 0
      area.rows.forEach(row => {
        row.seats.forEach(seat => {
          if (!seat.sold) count++
        })
      })
      return count
    },
    isSelected(seatId) {
      return this.selectedSeats.some(s => s.id === seatId)
    },
    toggleSeat(seat) {
      if (seat.sold) return
      const index = this.selectedSeats.findIndex(s => s.id === seat.id)
      if (index > -1) {
        this.selectedSeats.splice(index, 1)
      } else {
        if (this.selectedSeats.length >= this.maxSelect) {
          this.$message.warning(`最多选择${this.maxSelect}个座位`)
          return
        }
        this.selectedSeats.push(seat)
      }
    },
    removeSeat(seat) {
      const index = this.selectedSeats.findIndex(s => s.id === seat.id)
      if (index > -1) {
        this.selectedSeats.splice(index, 1)
      }
    },
    submitBuy() {
      if (this.selectedSeats.length === 0) {
        this.$message.warning('请先选择座位')
        return
      }
      this.dialogVisible = true
    },
    confirmOrder() {
      this.submitting = true
      createOrder({
        showId: parseInt(this.showId),
        ticketTypeId: this.selectedArea.ticketTypeId,
        seatIds: this.selectedSeats.map(s => s.id)
      }).then(res => {
        this.$message.success('下单成功！')
        this.dialogVisible = false
        this.selectedSeats = []
        this.$router.push(`/order/detail/${res.data.id}`)
      }).catch(() => {}).finally(() => {
        this.submitting = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.seat-page {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
  min-height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
}

.seat-header {
  margin-bottom: 20px;
  .show-info {
    margin-top: 10px;
    .venue-name {
      color: #909399;
      font-size: 14px;
    }
  }
}

.area-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.area-tab {
  padding: 12px 20px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
  background: #fff;
  min-width: 120px;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  }

  &.active {
    border-color: #409eff;
    background: #ecf5ff;
  }

  &.soldout {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .area-name {
    display: block;
    font-weight: bold;
    font-size: 15px;
    margin-bottom: 4px;
  }

  .area-price {
    display: block;
    color: #f56c6c;
    font-size: 18px;
    font-weight: bold;
  }

  .area-stock {
    display: block;
    color: #909399;
    font-size: 12px;
    margin-top: 4px;
  }
}

.seat-stage {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stage-label {
  text-align: center;
  padding: 8px 0;
  background: linear-gradient(90deg, #e6e8eb, #d3d6db, #e6e8eb);
  color: #606266;
  font-size: 14px;
  letter-spacing: 8px;
  border-radius: 4px;
  margin-bottom: 24px;
}

.seat-grid-wrapper {
  overflow-x: auto;
  padding: 10px 0;
}

.seat-grid {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: fit-content;
  margin: 0 auto;
}

.seat-row {
  display: flex;
  align-items: center;
  gap: 6px;

  .row-label {
    width: 28px;
    text-align: right;
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
  }
}

.seat {
  width: 32px;
  height: 32px;
  border-radius: 6px 6px 3px 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 11px;
  border: 1px solid transparent;

  &.available {
    background: #e1f3d8;
    border-color: #67c23a;

    &:hover {
      background: #b3e19d;
      transform: scale(1.1);
    }
  }

  &.selected {
    background: #409eff;
    border-color: #337ecc;
    color: #fff;
    transform: scale(1.05);
    box-shadow: 0 2px 6px rgba(64, 158, 255, 0.4);
  }

  &.sold {
    background: #f5f7fa;
    border-color: #e4e7ed;
    color: #c0c4cc;
    cursor: not-allowed;
  }

  .seat-number {
    font-size: 10px;
  }
}

.seat-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #606266;
  }

  .seat-icon {
    width: 18px;
    height: 18px;
    border-radius: 4px 4px 2px 2px;
    display: inline-block;

    &.available { background: #e1f3d8; border: 1px solid #67c23a; }
    &.selected { background: #409eff; }
    &.sold { background: #f5f7fa; border: 1px solid #e4e7ed; }
  }
}

.seat-footer {
  position: sticky;
  bottom: 0;
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.08);
  z-index: 10;
  border-radius: 0 0 8px 8px;

  .selected-info {
    flex: 1;
    margin-right: 20px;

    .selected-seats {
      margin-bottom: 6px;
    }

    .price-summary {
      display: flex;
      gap: 16px;
      font-size: 14px;

      .total-price strong {
        color: #f56c6c;
        font-size: 20px;
      }
    }
  }

  .submit-btn {
    min-width: 160px;
    height: 48px;
    font-size: 16px;
    border-radius: 8px;
  }
}

.confirm-order {
  .confirm-item {
    display: flex;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
    font-size: 15px;

    .label { color: #909399; }
    .price { color: #f56c6c; font-weight: bold; }

    &.total {
      border-bottom: none;
      font-size: 18px;
      .total-price { font-size: 22px; }
    }
  }
}

@media (max-width: 768px) {
  .seat-page { padding: 10px; }
  .area-tabs { gap: 8px; }
  .area-tab { padding: 8px 12px; min-width: 90px; }
  .area-tab .area-price { font-size: 15px; }
  .seat { width: 26px; height: 26px; }
  .seat .seat-number { font-size: 9px; }
  .seat-footer .submit-btn { min-width: 120px; font-size: 14px; }
}
</style>
