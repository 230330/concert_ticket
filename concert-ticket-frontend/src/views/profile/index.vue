<template>
  <div class="app-container" v-loading="loading">
    <!-- 个人信息卡片 -->
    <el-card>
      <div slot="header">
        <span>个人信息</span>
      </div>
      <el-form :model="profileForm" label-width="100px" style="max-width:500px">
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="profileForm.nickname"
            placeholder="请输入昵称"
            :disabled="nicknameLocked"
            maxlength="30"
          />
          <div v-if="nicknameLocked" class="form-tip">
            昵称每月仅可修改一次，下次可修改时间：{{ nextNicknameTime }}
          </div>
        </el-form-item>
        <el-form-item label="头像">
          <el-input v-model="profileForm.avatar" placeholder="请输入头像URL" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveProfile" :loading="saveLoading">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码卡片 -->
    <el-card style="margin-top:20px">
      <div slot="header">
        <span>修改密码</span>
      </div>
      <el-form
        ref="passwordForm"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        style="max-width:500px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（8-20位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleChangePassword" :loading="pwdLoading">修改密码</el-button>
          <el-button @click="resetPasswordForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getInfo, updateInfo, changePassword } from '@/api/user'

export default {
  name: 'ProfileIndex',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }

    return {
      profileForm: { phone: '', nickname: '', avatar: '' },
      passwordForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
      loading: false,
      saveLoading: false,
      pwdLoading: false,
      nicknameLastModified: null,
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度为8-20个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    // 昵称是否被锁定（一个月内已修改过）
    nicknameLocked() {
      if (!this.nicknameLastModified) return false
      const nextAllowed = new Date(this.nicknameLastModified)
      nextAllowed.setMonth(nextAllowed.getMonth() + 1)
      return new Date() < nextAllowed
    },
    // 下次可修改昵称的时间
    nextNicknameTime() {
      if (!this.nicknameLastModified) return ''
      const nextAllowed = new Date(this.nicknameLastModified)
      nextAllowed.setMonth(nextAllowed.getMonth() + 1)
      const y = nextAllowed.getFullYear()
      const m = String(nextAllowed.getMonth() + 1).padStart(2, '0')
      const d = String(nextAllowed.getDate()).padStart(2, '0')
      const h = String(nextAllowed.getHours()).padStart(2, '0')
      const min = String(nextAllowed.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${d} ${h}:${min}`
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
        this.profileForm.phone = d.phone
        this.profileForm.nickname = d.nickname || ''
        this.profileForm.avatar = d.avatar || ''
        this.nicknameLastModified = d.nicknameLastModified || null
      }).finally(() => { this.loading = false })
    },
    handleSaveProfile() {
      this.saveLoading = true
      updateInfo({
        nickname: this.profileForm.nickname,
        avatar: this.profileForm.avatar
      }).then(() => {
        this.$message.success('保存成功')
        // 重新获取用户信息以刷新昵称锁定状态
        this.fetchInfo()
        // 更新 store 中的昵称
        this.$store.dispatch('user/getInfo')
      }).finally(() => { this.saveLoading = false })
    },
    handleChangePassword() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) return
        this.pwdLoading = true
        changePassword({
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword,
          confirmPassword: this.passwordForm.confirmPassword
        }).then(() => {
          this.$message.success('密码修改成功，请重新登录')
          this.resetPasswordForm()
          // 修改密码后退出登录
          this.$store.dispatch('user/logout').then(() => {
            this.$router.push('/login')
          })
        }).finally(() => { this.pwdLoading = false })
      })
    },
    resetPasswordForm() {
      this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
      this.$refs.passwordForm && this.$refs.passwordForm.resetFields()
    }
  }
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #E6A23C;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
