<template>
  <div class="register-container">
    <div class="register-bg">
      <div class="bg-pattern"></div>
    </div>
    <div class="register-card">
      <div class="register-header">
        <div class="logo-icon"><i class="el-icon-headset"></i></div>
        <h2 class="register-title">用户注册</h2>
        <p class="register-subtitle">注册账号，畅享精彩演出</p>
      </div>

      <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="register-form" label-position="top">
        <el-form-item prop="phone" label="手机号">
          <el-input
            v-model="registerForm.phone"
            placeholder="请输入手机号"
            maxlength="11"
            prefix-icon="el-icon-mobile-phone"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password" label="密码">
          <el-input
            v-model="registerForm.password"
            :type="passwordType"
            placeholder="8-32位，包含字母和数字"
            prefix-icon="el-icon-lock"
          >
            <template #suffix>
              <i class="el-icon-view toggle-pwd" @click="showPwd"></i>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="confirmPassword" label="确认密码">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="el-icon-lock"
          />
        </el-form-item>

        <el-form-item prop="code" label="验证码">
          <div class="sms-row">
            <el-input
              v-model="registerForm.code"
              placeholder="请输入验证码"
              maxlength="6"
              prefix-icon="el-icon-message"
              style="flex:1"
            />
            <el-button
              :loading="smsLoading"
              :disabled="smsCountdown > 0"
              type="primary"
              plain
              style="margin-left:10px;width:130px;border-radius:10px"
              @click="handleSendSms"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-button :loading="loading" type="primary" class="register-btn" @click.native.prevent="handleRegister">注 册</el-button>

        <div class="register-footer">
          <span>已有账号？<el-link type="primary" @click="$router.push('/login')">立即登录</el-link></span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { validPhone, validPassword } from '@/utils/validate'
import { sendSms, register } from '@/api/user'

export default {
  name: 'Register',
  data() {
    const validatePhone = (rule, value, callback) => {
      if (!validPhone(value)) {
        callback(new Error('请输入正确的手机号'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (!validPassword(value)) {
        callback(new Error('密码需8-32位，且必须包含字母和数字'))
      } else {
        callback()
      }
    }
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      registerForm: {
        phone: '',
        password: '',
        confirmPassword: '',
        code: ''
      },
      registerRules: {
        phone: [{ required: true, trigger: 'blur', validator: validatePhone }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }],
        confirmPassword: [{ required: true, trigger: 'blur', validator: validateConfirmPassword }],
        code: [{ required: true, trigger: 'blur', message: '请输入验证码' }]
      },
      loading: false,
      smsLoading: false,
      smsCountdown: 0,
      smsTimer: null,
      passwordType: 'password'
    }
  },
  beforeDestroy() {
    if (this.smsTimer) {
      clearInterval(this.smsTimer)
    }
  },
  methods: {
    showPwd() {
      this.passwordType = this.passwordType === 'password' ? '' : 'password'
    },
    handleSendSms() {
      this.$refs.registerForm.validateField('phone', (errMsg) => {
        if (errMsg) return
        this.smsLoading = true
        sendSms({ phone: this.registerForm.phone }).then(() => {
          this.$message.success('验证码已发送')
          this.smsCountdown = 60
          this.smsTimer = setInterval(() => {
            this.smsCountdown--
            if (this.smsCountdown <= 0) {
              clearInterval(this.smsTimer)
              this.smsTimer = null
            }
          }, 1000)
        }).catch(() => {}).finally(() => {
          this.smsLoading = false
        })
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate((valid) => {
        if (valid) {
          this.loading = true
          register({
            phone: this.registerForm.phone,
            password: this.registerForm.password,
            code: this.registerForm.code
          }).then(() => {
            this.$message.success('注册成功，请登录')
            this.$router.push('/login')
          }).catch(() => {}).finally(() => {
            this.loading = false
          })
        } else {
          return false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.register-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.register-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  z-index: 0;

  .bg-pattern {
    width: 100%;
    height: 100%;
    opacity: 0.08;
    background-image: radial-gradient(circle at 20% 50%, #fff 1px, transparent 1px),
      radial-gradient(circle at 80% 20%, #fff 1px, transparent 1px);
    background-size: 60px 60px, 80px 80px;
  }
}

.register-card {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: 90vw;
  background: #fff;
  border-radius: 20px;
  padding: 40px 36px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.register-header {
  text-align: center;
  margin-bottom: 28px;

  .logo-icon {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 12px;

    i { font-size: 28px; color: #fff; }
  }

  .register-title {
    font-size: 22px;
    color: #303133;
    margin: 0 0 6px 0;
    font-weight: bold;
  }

  .register-subtitle {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

.register-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #606266;
  }

  :deep(.el-input__inner) {
    height: 44px;
    border-radius: 10px;

    &:focus { border-color: #667eea; }
  }

  .toggle-pwd {
    cursor: pointer;
    padding: 0 10px;
    line-height: 44px;
  }

  .sms-row {
    display: flex;
    align-items: flex-start;
  }
}

.register-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  margin-top: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;

  &:hover { opacity: 0.9; }
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #909399;
}

@media (max-width: 480px) {
  .register-card { padding: 28px 20px 20px; border-radius: 16px; }
  .register-header .register-title { font-size: 18px; }
}
</style>
