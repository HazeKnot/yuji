<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <h2>育己 · 机构工作台</h2>
      <p class="sub">妇幼保健院 / 月子中心 / 社区服务中心</p>
      <el-form :model="form" label-position="top" @submit.prevent="onLogin">
        <el-form-item label="机构账号">
          <el-input v-model="form.account" placeholder="如 org001" @keyup.enter="onLogin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            @keyup.enter="onLogin"
          />
        </el-form-item>
        <el-button type="primary" style="width: 100%" :loading="loading" native-type="submit">
          登 录
        </el-button>
      </el-form>
      <p class="tip">演示账号：org001 / org123456</p>
      <p class="tip dim">仅机构账号可进入；妈妈账号请用手机端</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api, saveSession } from '../api'

interface LoginUser {
  id: number
  phone: string
  nickname: string
  role: string
}

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ account: '', password: '' })

// H5 登录页「机构登录」入口带票据跳转过来：直接接管并进入工作台
onMounted(() => {
  const token = route.query.token as string | undefined
  if (!token) return
  saveSession(token, {
    phone: '',
    nickname: (route.query.nickname as string) || '机构用户',
    role: (route.query.role as string) || 'ORG',
  })
  ElMessage.success('已登录机构工作台')
  router.replace('/')
})

async function onLogin() {
  if (!form.account.trim() || !form.password) {
    ElMessage.warning('请填写账号与密码')
    return
  }
  loading.value = true
  try {
    const res = await api.post<never, { data: { data: { token: string; user: LoginUser } } }>(
      '/auth/login',
      { phone: form.account.trim(), password: form.password },
    )
    const { token, user } = res.data.data
    if (user.role !== 'ORG' && user.role !== 'ADMIN') {
      ElMessage.error('该账号不是机构账号，请在用户端（H5）登录')
      return
    }
    saveSession(token, user)
    router.replace('/')
  } catch {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #eef3ec 0%, #f7f8f6 60%);
}
.login-card {
  width: 380px;
  text-align: center;
  border-radius: 14px;
}
.sub {
  color: #999;
  font-size: 13px;
}
.tip {
  color: #8a8a8a;
  font-size: 12px;
  margin-top: 14px;
  margin-bottom: 0;
}
.tip.dim {
  color: #bbb;
  margin-top: 6px;
}
</style>
