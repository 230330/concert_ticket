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
          <div class="avatar-upload-wrapper">
            <el-upload
              class="avatar-uploader"
              action=""
              :http-request="handleUploadAvatar"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              accept=".jpg,.jpeg,.png"
            >
              <img v-if="profileForm.avatar" :src="avatarFullUrl" class="avatar-preview" />
              <i v-else class="el-icon-plus avatar-uploader-icon"></i>
            </el-upload>
            <div class="avatar-tip">支持JPG、PNG格式，大小不超过10MB</div>
          </div>
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
            placeholder="请输入新密码（8-32位，含字母和数字）"
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
import { getInfo, updateInfo, changePassword, uploadAvatar } from '@/api/user'

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
      uploadLoading: false,
      nicknameLastModified: null,
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 8, max: 32, message: '密码长度为8-32个字符', trigger: 'blur' }
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
    },
    // 头像完整URL：处理相对路径
    avatarFullUrl() {
      if (!this.profileForm.avatar) return ''
      // 如果是完整URL直接返回
      if (this.profileForm.avatar.startsWith('http')) return this.profileForm.avatar
      // 相对路径拼接后端地址
      return process.env.VUE_APP_BASE_API + this.profileForm.avatar
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
    /**
     * 上传前校验：格式和大小
     */
    beforeAvatarUpload(file) {
      const isJpgOrPng = ['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)
      if (!isJpgOrPng) {
        this.$message.error('头像仅支持JPG和PNG格式！')
        return false
      }
      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        this.$message.error('头像图片大小不能超过10MB！')
        return false
      }
      return true
    },
    /**
     * 自定义上传头像
     */
    handleUploadAvatar(options) {
      this.uploadLoading = true
      uploadAvatar(options.file).then(res => {
        this.profileForm.avatar = res.data
        this.$message.success('头像上传成功')
      }).catch(() => {
        this.$message.error('头像上传失败')
      }).finally(() => {
        this.uploadLoading = false
      })
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
        // 更新 store 中的昵称和头像
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

.avatar-upload-wrapper {
  display: flex;
  align-items: flex-end;
}

.avatar-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 16px;
  line-height: 1.5;
}

.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  object-fit: cover;
  display: block;
}
</style>
