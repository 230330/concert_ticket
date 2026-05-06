<template>
  <div class="forgot-container">
    <el-form ref="forgotForm" :model="forgotForm" :rules="forgotRules" class="forgot-form" auto-complete="on" label-position="left">
      <div class="title-container">
        <h3 class="title">忘记密码</h3>
      </div>

      <el-form-item prop="phone">
        <span class="svg-container">
          <svg-icon icon-class="user" />
        </span>
        <el-input
          ref="phone"
          v-model="forgotForm.phone"
          placeholder="请输入注册手机号"
          name="phone"
          type="text"
          tabindex="1"
          auto-complete="on"
          maxlength="11"
        />
      </el-form-item>

      <el-form-item prop="code">
        <div style="display:flex">
          <span class="svg-container">
            <svg-icon icon-class="password" />
          </span>
          <el-input
            ref="code"
            v-model="forgotForm.code"
            placeholder="验证码"
            name="code"
            tabindex="2"
            auto-complete="on"
            maxlength="6"
            style="flex:1"
          />
          <el-button
            :loading="smsLoading"
            :disabled="smsCountdown > 0"
            type="primary"
            style="margin-left:10px;width:130px"
            @click="handleSendSms"
          >
            {{ smsCountdown > 0 ? `${smsCountdown}s 后重发` : '获取验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <el-form-item prop="newPassword">
        <span class="svg-container">
          <svg-icon icon-class="password" />
        </span>
        <el-input
          :key="newPasswordType"
          ref="newPassword"
          v-model="forgotForm.newPassword"
          :type="newPasswordType"
          placeholder="请输入新密码（8-32位，含字母和数字）"
          name="newPassword"
          tabindex="3"
          auto-complete="on"
        />
        <span class="show-pwd" @click="showNewPwd">
          <svg-icon :icon-class="newPasswordType === 'password' ? 'eye' : 'eye-open'" />
        </span>
      </el-form-item>

      <el-form-item prop="confirmPassword">
        <span class="svg-container">
          <svg-icon icon-class="password" />
        </span>
        <el-input
          :key="confirmPasswordType"
          ref="confirmPassword"
          v-model="forgotForm.confirmPassword"
          :type="confirmPasswordType"
          placeholder="请确认新密码"
          name="confirmPassword"
          tabindex="4"
          auto-complete="on"
        />
        <span class="show-pwd" @click="showConfirmPwd">
          <svg-icon :icon-class="confirmPasswordType === 'password' ? 'eye' : 'eye-open'" />
        </span>
      </el-form-item>

      <el-button :loading="loading" type="primary" style="width:100%;margin-bottom:30px" @click.native.prevent="handleReset">重置密码</el-button>

      <div class="tips">
        <span>想起密码？<el-link type="primary" @click="$router.push('/login')">返回登录</el-link></span>
      </div>
    </el-form>
  </div>
</template>

<script>
import { validPhone, validPassword } from '@/utils/validate'
import { sendResetSms, resetPassword } from '@/api/user'

export default {
  name: 'ForgotPassword',
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
      if (value !== this.forgotForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      forgotForm: {
        phone: '',
        code: '',
        newPassword: '',
        confirmPassword: ''
      },
      forgotRules: {
        phone: [{ required: true, trigger: 'blur', validator: validatePhone }],
        code: [{ required: true, trigger: 'blur', message: '请输入验证码' }],
        newPassword: [{ required: true, trigger: 'blur', validator: validatePassword }],
        confirmPassword: [{ required: true, trigger: 'blur', validator: validateConfirmPassword }]
      },
      loading: false,
      smsLoading: false,
      smsCountdown: 0,
      smsTimer: null,
      newPasswordType: 'password',
      confirmPasswordType: 'password'
    }
  },
  beforeDestroy() {
    if (this.smsTimer) {
      clearInterval(this.smsTimer)
    }
  },
  methods: {
    showNewPwd() {
      this.newPasswordType = this.newPasswordType === 'password' ? '' : 'password'
      this.$nextTick(() => {
        this.$refs.newPassword.focus()
      })
    },
    showConfirmPwd() {
      this.confirmPasswordType = this.confirmPasswordType === 'password' ? '' : 'password'
      this.$nextTick(() => {
        this.$refs.confirmPassword.focus()
      })
    },
    handleSendSms() {
      // 先单独校验手机号
      this.$refs.forgotForm.validateField('phone', (errMsg) => {
        if (errMsg) return
        this.smsLoading = true
        sendResetSms({ phone: this.forgotForm.phone }).then(() => {
          this.$message.success('验证码已发送')
          // 开始倒计时（60秒）
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
    handleReset() {
      this.$refs.forgotForm.validate((valid) => {
        if (valid) {
          this.loading = true
          resetPassword({
            phone: this.forgotForm.phone,
            code: this.forgotForm.code,
            newPassword: this.forgotForm.newPassword,
            confirmPassword: this.forgotForm.confirmPassword
          }).then(() => {
            this.$message.success('密码重置成功，请登录')
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

<style lang="scss">
/* 修复input 背景不协调 和光标变色 */
$bg: #283443;
$light_gray: #fff;
$cursor: #fff;

@supports (-webkit-mask: none) and (not (cater-color: $cursor)) {
  .forgot-container .el-input input {
    color: $cursor;
  }
}

/* reset element-ui css */
.forgot-container {
  .el-input {
    display: inline-block;
    height: 47px;
    width: 85%;

    input {
      background: transparent;
      border: 0px;
      -webkit-appearance: none;
      border-radius: 0px;
      padding: 12px 5px 12px 15px;
      color: $light_gray;
      height: 47px;
      caret-color: $cursor;

      &:-webkit-autofill {
        box-shadow: 0 0 0px 1000px $bg inset !important;
        -webkit-text-fill-color: $cursor !important;
      }
    }
  }

  .el-form-item {
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.1);
    border-radius: 5px;
    color: #454545;
  }
}
</style>

<style lang="scss" scoped>
$bg: #2d3a4b;
$dark_gray: #889aa4;
$light_gray: #eee;

.forgot-container {
  min-height: 100%;
  width: 100%;
  background-color: $bg;
  overflow: hidden;

  .forgot-form {
    position: relative;
    width: 520px;
    max-width: 100%;
    padding: 100px 35px 0;
    margin: 0 auto;
    overflow: hidden;
  }

  .tips {
    font-size: 14px;
    color: #fff;
    margin-bottom: 10px;
    text-align: center;
  }

  .svg-container {
    padding: 6px 5px 6px 15px;
    color: $dark_gray;
    vertical-align: middle;
    width: 30px;
    display: inline-block;
  }

  .title-container {
    position: relative;

    .title {
      font-size: 26px;
      color: $light_gray;
      margin: 0px auto 40px auto;
      text-align: center;
      font-weight: bold;
    }
  }

  .show-pwd {
    position: absolute;
    right: 10px;
    top: 7px;
    font-size: 16px;
    color: $dark_gray;
    cursor: pointer;
    user-select: none;
  }
}
</style>
