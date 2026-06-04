<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="bg-pattern"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon"><i class="el-icon-headset"></i></div>
        <h2 class="login-title">演唱会售票系统</h2>
        <p class="login-subtitle">精彩演出，触手可及</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        label-position="top"
      >
        <el-form-item prop="phone" label="手机号">
          <el-input
            v-model="loginForm.phone"
            placeholder="请输入手机号"
            name="phone"
            type="text"
            tabindex="1"
            maxlength="11"
            prefix-icon="el-icon-mobile-phone"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password" label="密码">
          <el-input
            v-model="loginForm.password"
            :type="passwordType"
            placeholder="请输入密码"
            name="password"
            tabindex="2"
            prefix-icon="el-icon-lock"
            @keyup.enter="handleLogin"
          >
            <template #suffix>
              <i class="el-icon-view toggle-pwd" @click="showPwd"></i>
            </template>
          </el-input>
        </el-form-item>

        <el-button
          :loading="loading"
          type="primary"
          class="login-btn"
          @click.prevent="handleLogin"
        >登 录</el-button>

        <div class="login-footer">
          <span>没有账号？<el-link type="primary" @click="$router.push('/register')">立即注册</el-link></span>
          <el-link type="info" @click="$router.push('/forgot-password')">忘记密码？</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { validPhone, validPassword } from '@/utils/validate'
import { useUserStore } from '@/store/modules/user'

export default {
  name: 'Login',
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
        callback(new Error('密码需8-20位，且必须包含字母和数字'))
      } else {
        callback()
      }
    }
    return {
      loginForm: {
        phone: '',
        password: ''
      },
      loginRules: {
        phone: [{ required: true, trigger: 'blur', validator: validatePhone }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      loading: false,
      passwordType: 'password',
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  methods: {
    showPwd() {
      this.passwordType = this.passwordType === 'password' ? '' : 'password'
    },
    handleLogin() {
      this.$refs.loginFormRef.validate((valid) => {
        if (valid) {
          this.loading = true
          const userStore = useUserStore()
          userStore.loginAction(this.loginForm)
            .then(() => {
              this.$router.push({ path: this.redirect || '/' })
              this.loading = false
            })
            .catch(() => {
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
.login-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-bg {
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
      radial-gradient(circle at 80% 20%, #fff 1px, transparent 1px),
      radial-gradient(circle at 60% 80%, #fff 1px, transparent 1px);
    background-size: 60px 60px, 80px 80px, 100px 100px;
  }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: 90vw;
  background: #fff;
  border-radius: 20px;
  padding: 48px 36px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo-icon {
    width: 64px;
    height: 64px;
    border-radius: 16px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;

    i {
      font-size: 32px;
      color: #fff;
    }
  }

  .login-title {
    font-size: 24px;
    color: #303133;
    margin: 0 0 8px 0;
    font-weight: bold;
  }

  .login-subtitle {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}

.login-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #606266;
  }

  :deep(.el-input__inner) {
    height: 44px;
    border-radius: 10px;
    border-color: #dcdfe6;

    &:focus {
      border-color: #667eea;
    }
  }

  :deep(.el-input__prefix) {
    left: 10px;
  }

  .toggle-pwd {
    cursor: pointer;
    padding: 0 10px;
    line-height: 44px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  font-size: 16px;
  margin-top: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;

  &:hover {
    opacity: 0.9;
  }
}

.login-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  font-size: 14px;
  color: #909399;
}

@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px 24px;
    border-radius: 16px;
  }

  .login-header .logo-icon {
    width: 52px;
    height: 52px;

    i { font-size: 26px; }
  }

  .login-header .login-title { font-size: 20px; }
}
</style>
