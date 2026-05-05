<template>
  <div class="app-container" v-loading="loading">
    <el-card>
      <div slot="header">
        <span>个人信息</span>
      </div>
      <el-form :model="form" label-width="100px" style="max-width:500px">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="头像">
          <el-input v-model="form.avatar" placeholder="请输入头像URL" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getInfo, updateInfo } from '@/api/user'

export default {
  name: 'ProfileIndex',
  data() {
    return {
      form: { phone: '', nickname: '', avatar: '' },
      loading: false
    }
  },
  created() {
    this.fetchInfo()
  },
  methods: {
    fetchInfo() {
      this.loading = true
      getInfo().then(res => {
        const d = res.data
        this.form.phone = d.phone
        this.form.nickname = d.nickname || ''
        this.form.avatar = d.avatar || ''
      }).finally(() => { this.loading = false })
    },
    handleSave() {
      updateInfo({ nickname: this.form.nickname, avatar: this.form.avatar }).then(() => {
        this.$message.success('保存成功')
      })
    }
  }
}
</script>
